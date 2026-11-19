package com.mastercode.app.data.duelos

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.conAccess
import java.io.IOException

// Lo que pasó con una llamada a los duelos. Rechazado lleva el código con el que user-api dijo que no (400, 403, 404, 409
// o 503): cada pantalla lo traduce a su mensaje. Cualquier otro error del servidor es ErrorInesperado.
sealed interface ResultadoDuelo<out T> {
    data class Exito<T>(val valor: T) : ResultadoDuelo<T>
    data class Rechazado(val codigo: Int) : ResultadoDuelo<Nothing>
    data object SesionVencida : ResultadoDuelo<Nothing>
    data object SinConexion : ResultadoDuelo<Nothing>
    data object ErrorInesperado : ResultadoDuelo<Nothing>
}

// El token sale de AuthRepository, como en los demás repositorios.
class DuelosRepository(private val api: DuelosApi, private val auth: AuthRepository) {

    suspend fun companeros(cursoId: Long) = llamar { api.companeros(it, cursoId) }

    suspend fun retar(cursoId: Long, rivalId: Long) = llamar { api.retar(it, cursoId, rivalId) }

    suspend fun invitaciones() = llamar { api.invitaciones(it) }

    suspend fun aceptar(partidaId: Long) = llamar { api.aceptar(it, partidaId) }

    suspend fun rechazar(partidaId: Long) = llamar { api.rechazar(it, partidaId) }

    // El valor es null cuando el estudiante ya respondió todo y espera al rival (204).
    suspend fun ejercicio(partidaId: Long) = llamar { api.ejercicio(it, partidaId) }

    suspend fun responder(partidaId: Long, turno: Int, texto: String?, opcionId: Long?) =
        llamar { api.responder(it, partidaId, turno, texto, opcionId) }

    suspend fun marcador(partidaId: Long, etag: String?) = llamar { api.marcador(it, partidaId, etag) }

    suspend fun historial(pagina: Int, tamano: Int) = llamar { api.historial(it, pagina, tamano) }

    suspend fun ranking(cursoId: Long, pagina: Int, tamano: Int) = llamar { api.ranking(it, cursoId, pagina, tamano) }

    private suspend fun <T> llamar(pedir: suspend (access: String) -> T): ResultadoDuelo<T> = auth.conAccess(
        vencida = ResultadoDuelo.SesionVencida,
        sinConexion = ResultadoDuelo.SinConexion,
        inesperado = ResultadoDuelo.ErrorInesperado,
    ) { access ->
        try {
            ResultadoDuelo.Exito(pedir(access))
        } catch (e: ErrorHttp) {
            when (e.codigo) {
                401 -> null
                400, 403, 404, 409, 503 -> ResultadoDuelo.Rechazado(e.codigo)
                else -> ResultadoDuelo.ErrorInesperado
            }
        } catch (e: IOException) {
            ResultadoDuelo.SinConexion
        }
    }
}
