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

sealed interface ResultadoEstadoDeSesion {
    data class Exito(val estado: EstadoDeSesionServidor) : ResultadoEstadoDeSesion
    // 404: la sesión no existe o no es del estudiante.
    data object NoExiste : ResultadoEstadoDeSesion
    data object SesionVencida : ResultadoEstadoDeSesion
    data object SinConexion : ResultadoEstadoDeSesion
    data object ErrorInesperado : ResultadoEstadoDeSesion
}

sealed interface ResultadoTerminar {
    data class Exito(val resumen: ResumenDeSesion) : ResultadoTerminar
    // 404 o 409: la sesión no existe, no es del estudiante o ya estaba terminada.
    data object YaTerminada : ResultadoTerminar
    data object SesionVencida : ResultadoTerminar
    data object SinConexion : ResultadoTerminar
    data object ErrorInesperado : ResultadoTerminar
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

    // HU55-T2: cómo va una sesión guardada, para ofrecer continuarla.
    suspend fun estado(sesionId: Long): ResultadoEstadoDeSesion = auth.conAccess(
        vencida = ResultadoEstadoDeSesion.SesionVencida,
        sinConexion = ResultadoEstadoDeSesion.SinConexion,
        inesperado = ResultadoEstadoDeSesion.ErrorInesperado,
    ) { access ->
        try {
            ResultadoEstadoDeSesion.Exito(api.sesion(access, sesionId))
        } catch (e: ErrorHttp) {
            when (e.codigo) {
                401 -> null
                404 -> ResultadoEstadoDeSesion.NoExiste
                else -> ResultadoEstadoDeSesion.ErrorInesperado
            }
        } catch (e: IOException) {
            ResultadoEstadoDeSesion.SinConexion
        }
    }

    // HU55-T3: termina la sesión y trae su resumen.
    suspend fun terminar(sesionId: Long): ResultadoTerminar = auth.conAccess(
        vencida = ResultadoTerminar.SesionVencida,
        sinConexion = ResultadoTerminar.SinConexion,
        inesperado = ResultadoTerminar.ErrorInesperado,
    ) { access ->
        try {
            ResultadoTerminar.Exito(api.terminar(access, sesionId))
        } catch (e: ErrorHttp) {
            when (e.codigo) {
                401 -> null
                404, 409 -> ResultadoTerminar.YaTerminada
                else -> ResultadoTerminar.ErrorInesperado
            }
        } catch (e: IOException) {
            ResultadoTerminar.SinConexion
        }
    }
}
