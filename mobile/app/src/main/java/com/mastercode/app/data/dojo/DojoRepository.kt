package com.mastercode.app.data.dojo

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.conAccess
import com.mastercode.app.data.problemas.ProblemaDelEjercicio
import java.io.IOException

sealed interface ResultadoInicioDojo {
    data class Exito(val sesion: SesionIniciada) : ResultadoInicioDojo
    data object SesionVencida : ResultadoInicioDojo
    data object SinConexion : ResultadoInicioDojo
    data object ErrorInesperado : ResultadoInicioDojo
}

sealed interface ResultadoSiguiente {
    data class Exito(val problema: ProblemaDelEjercicio) : ResultadoSiguiente
    // 204: no quedan ejercicios para esta sesión.
    data object SinMas : ResultadoSiguiente
    // 404 o 409: la sesión no existe, no es del estudiante o ya terminó.
    data object SesionTerminada : ResultadoSiguiente
    data object SesionVencida : ResultadoSiguiente
    data object SinConexion : ResultadoSiguiente
    data object ErrorInesperado : ResultadoSiguiente
}

// El token sale de AuthRepository, como en los demás repositorios.
class DojoRepository(private val api: DojoApi, private val auth: AuthRepository) {

    suspend fun iniciar(lenguaje: String): ResultadoInicioDojo = auth.conAccess(
        vencida = ResultadoInicioDojo.SesionVencida,
        sinConexion = ResultadoInicioDojo.SinConexion,
        inesperado = ResultadoInicioDojo.ErrorInesperado,
    ) { access ->
        try {
            ResultadoInicioDojo.Exito(api.iniciar(access, lenguaje))
        } catch (e: ErrorHttp) {
            if (e.codigo == 401) null else ResultadoInicioDojo.ErrorInesperado
        } catch (e: IOException) {
            ResultadoInicioDojo.SinConexion
        }
    }

    suspend fun siguiente(sesionId: Long): ResultadoSiguiente = auth.conAccess(
        vencida = ResultadoSiguiente.SesionVencida,
        sinConexion = ResultadoSiguiente.SinConexion,
        inesperado = ResultadoSiguiente.ErrorInesperado,
    ) { access ->
        try {
            api.siguiente(access, sesionId)?.let { ResultadoSiguiente.Exito(it) } ?: ResultadoSiguiente.SinMas
        } catch (e: ErrorHttp) {
            when (e.codigo) {
                401 -> null
                404, 409 -> ResultadoSiguiente.SesionTerminada
                else -> ResultadoSiguiente.ErrorInesperado
            }
        } catch (e: IOException) {
            ResultadoSiguiente.SinConexion
        }
    }
}
