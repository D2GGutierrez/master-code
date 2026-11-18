package com.mastercode.app.data.dispositivo

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.conAccess
import java.io.IOException

// De dónde sale el token de Firebase. Hoy no hay SDK de Firebase en la app (falta aprobar las dependencias y crear el
// proyecto, ver docs/notificaciones-push.md), así que la implementación por defecto no tiene token. Cuando se agregue
// Firebase, la implementación llama a FirebaseMessaging.getInstance().token y el resto no cambia.
interface ProveedorDeToken {
    // null si todavía no hay token (sin Firebase, o el teléfono no lo dio).
    suspend fun obtener(): String?
}

object SinFirebase : ProveedorDeToken {
    override suspend fun obtener(): String? = null
}

// El token que quedó sin enviar (sin conexión o con la sesión vencida), para mandarlo en cuanto se pueda. Un token nuevo
// que Firebase entrega con la app cerrada no puede perderse por no haber señal en ese momento.
interface AlmacenDeToken {
    fun leerPendiente(): String?
    fun guardarPendiente(token: String?)
}

sealed interface ResultadoDispositivo {
    data object Registrado : ResultadoDispositivo
    // No hay token que mandar todavía (sin Firebase).
    data object SinToken : ResultadoDispositivo
    // 400: user-api no acepta ese token. No se reintenta: el mismo token seguiría fallando.
    data object TokenRechazado : ResultadoDispositivo
    data object SesionVencida : ResultadoDispositivo
    data object SinConexion : ResultadoDispositivo
    data object ErrorInesperado : ResultadoDispositivo
}

// HU52-T1: registra el token del teléfono en user-api. Se llama al abrir la app con la sesión iniciada y cuando Firebase
// renueva el token (alRenovarseElToken). PUT es idempotente: mandarlo de más no cambia nada. Un fallo no molesta a nadie.
class DispositivoRepository(
    private val api: DispositivoApi,
    private val auth: AuthRepository,
    private val proveedor: ProveedorDeToken,
    private val almacen: AlmacenDeToken,
) {

    // Manda el token que quedó pendiente o, si no hay, el que dé Firebase.
    suspend fun registrarElDelTelefono(): ResultadoDispositivo {
        val token = almacen.leerPendiente() ?: proveedor.obtener() ?: return ResultadoDispositivo.SinToken
        return enviar(token)
    }

    // Firebase entregó un token nuevo (FirebaseMessagingService.onNewToken).
    suspend fun alRenovarseElToken(token: String): ResultadoDispositivo = enviar(token)

    private suspend fun enviar(token: String): ResultadoDispositivo {
        val resultado = auth.conAccess(
            vencida = ResultadoDispositivo.SesionVencida,
            sinConexion = ResultadoDispositivo.SinConexion,
            inesperado = ResultadoDispositivo.ErrorInesperado,
        ) { access -> pedir(access, token) }
        // Solo se olvida el token si ya llegó o si el servidor lo rechazó; en cualquier otro caso queda para reintentar.
        when (resultado) {
            ResultadoDispositivo.Registrado, ResultadoDispositivo.TokenRechazado -> almacen.guardarPendiente(null)
            else -> almacen.guardarPendiente(token)
        }
        return resultado
    }

    // null significa 401: el access ya no sirve.
    private suspend fun pedir(access: String, token: String): ResultadoDispositivo? = try {
        api.registrar(access, token)
        ResultadoDispositivo.Registrado
    } catch (e: ErrorHttp) {
        when (e.codigo) {
            401 -> null
            400 -> ResultadoDispositivo.TokenRechazado
            else -> ResultadoDispositivo.ErrorInesperado
        }
    } catch (e: IOException) {
        ResultadoDispositivo.SinConexion
    }
}
