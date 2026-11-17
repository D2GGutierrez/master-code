package com.mastercode.app.data.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

sealed interface ResultadoLogin {
    data object Exito : ResultadoLogin
    data object CredencialesIncorrectas : ResultadoLogin
    data object PendienteActivacion : ResultadoLogin
    data object SinConexion : ResultadoLogin
    data object ErrorInesperado : ResultadoLogin
}

sealed interface ResultadoRenovacion {
    data object Exito : ResultadoRenovacion
    data object SesionExpirada : ResultadoRenovacion
    data object SinConexion : ResultadoRenovacion
    data object ErrorInesperado : ResultadoRenovacion
}

class AuthRepository(private val api: AuthApi, private val storage: TokenStorage) {

    // El access token solo vive en memoria: dura 15 minutos y se pide de nuevo con el refresh.
    var accessToken: String? = null
        private set

    fun haySesionGuardada(): Boolean = storage.leerRefresh() != null

    suspend fun login(email: String, password: String): ResultadoLogin = try {
        guardar(api.login(email, password))
        ResultadoLogin.Exito
    } catch (e: ErrorHttp) {
        when (e.codigo) {
            401 -> ResultadoLogin.CredencialesIncorrectas
            // admin-api responde 403 a la cuenta inactiva (y también a una cuenta sin rol, ver informe).
            403 -> ResultadoLogin.PendienteActivacion
            else -> ResultadoLogin.ErrorInesperado
        }
    } catch (e: IOException) {
        ResultadoLogin.SinConexion
    }

    // Una renovación a la vez: el refresh rota, y si dos pedidos (por ejemplo «Mis cursos» y el reenvío en segundo plano de
    // respuestas guardadas, HU48-T3) usaran el mismo a la vez, el segundo recibiría 401 y cerraría la sesión sin motivo.
    // Así el segundo espera y usa el refresh que dejó el primero.
    private val candadoDeRenovacion = Mutex()

    suspend fun renovar(): ResultadoRenovacion = candadoDeRenovacion.withLock { renovarSinCandado() }

    private suspend fun renovarSinCandado(): ResultadoRenovacion {
        val refresh = storage.leerRefresh() ?: return ResultadoRenovacion.SesionExpirada
        return try {
            // Rotación: el servidor invalida el refresh usado y entrega uno nuevo, que reemplaza al guardado.
            guardar(api.renovar(refresh))
            ResultadoRenovacion.Exito
        } catch (e: ErrorHttp) {
            if (e.codigo == 401) {
                // Vencido o invalidado: ya no sirve, se borra para volver a pedir login.
                cerrarSesion()
                ResultadoRenovacion.SesionExpirada
            } else {
                ResultadoRenovacion.ErrorInesperado
            }
        } catch (e: IOException) {
            // Sin red no se sabe si el refresh sigue vigente, así que se conserva.
            ResultadoRenovacion.SinConexion
        }
    }

    fun cerrarSesion() {
        accessToken = null
        storage.borrar()
    }

    private fun guardar(tokens: Tokens) {
        storage.guardarRefresh(tokens.refresh)
        accessToken = tokens.access
    }
}
