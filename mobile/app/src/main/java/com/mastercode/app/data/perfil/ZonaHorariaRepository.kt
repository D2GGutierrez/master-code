package com.mastercode.app.data.perfil

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.conAccess
import java.io.IOException
import java.util.TimeZone

sealed interface ResultadoZonaHoraria {
    data object Enviada : ResultadoZonaHoraria
    data object SesionVencida : ResultadoZonaHoraria
    data object SinConexion : ResultadoZonaHoraria
    data object ErrorInesperado : ResultadoZonaHoraria
}

// H6: manda a user-api la zona horaria del teléfono. Se llama al abrir la app con la sesión iniciada; si el estudiante
// viaja, la siguiente apertura la actualiza. Un fallo no molesta a nadie: se reintenta en la próxima apertura.
// zonaDelTelefono se puede reemplazar en las pruebas: en la JVM no hay un teléfono con zona propia.
class ZonaHorariaRepository(
    private val api: ZonaHorariaApi,
    private val auth: AuthRepository,
    private val zonaDelTelefono: () -> String = { TimeZone.getDefault().id },
) {

    suspend fun enviar(): ResultadoZonaHoraria {
        val zona = zonaDelTelefono()
        return auth.conAccess(
            vencida = ResultadoZonaHoraria.SesionVencida,
            sinConexion = ResultadoZonaHoraria.SinConexion,
            inesperado = ResultadoZonaHoraria.ErrorInesperado,
        ) { access -> pedir(access, zona) }
    }

    // null significa 401: el access ya no sirve.
    private suspend fun pedir(access: String, zona: String): ResultadoZonaHoraria? = try {
        api.guardar(access, zona)
        ResultadoZonaHoraria.Enviada
    } catch (e: ErrorHttp) {
        if (e.codigo == 401) null else ResultadoZonaHoraria.ErrorInesperado
    } catch (e: IOException) {
        ResultadoZonaHoraria.SinConexion
    }
}
