package com.mastercode.app.data.problemas

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.ResultadoRenovacion
import java.io.IOException

sealed interface ResultadoProblema {
    data class Exito(val problema: ProblemaDelEjercicio) : ResultadoProblema
    // 404: no existe, no está publicado o el estudiante no está inscrito en su curso (user-api no distingue cuál).
    data object NoEncontrado : ResultadoProblema
    data object SesionVencida : ResultadoProblema
    data object SinConexion : ResultadoProblema
    data object ErrorInesperado : ResultadoProblema
}

// El token sale de AuthRepository, el mismo que usa el login: no hay un segundo lugar con tokens.
class ProblemaRepository(private val api: ProblemaApi, private val auth: AuthRepository) {

    suspend fun problema(id: Long): ResultadoProblema {
        // Igual que MisCursosRepository: si el access falta o venció (401), se renueva y se pide una sola vez más.
        auth.accessToken?.let { access -> pedir(access, id)?.let { return it } }
        return when (auth.renovar()) {
            // Un 401 con un access recién emitido no es una sesión vencida: es otro problema.
            ResultadoRenovacion.Exito -> auth.accessToken?.let { pedir(it, id) } ?: ResultadoProblema.ErrorInesperado
            ResultadoRenovacion.SesionExpirada -> ResultadoProblema.SesionVencida
            ResultadoRenovacion.SinConexion -> ResultadoProblema.SinConexion
            ResultadoRenovacion.ErrorInesperado -> ResultadoProblema.ErrorInesperado
        }
    }

    // null significa 401: el access ya no sirve.
    private suspend fun pedir(access: String, id: Long): ResultadoProblema? = try {
        ResultadoProblema.Exito(api.problema(access, id))
    } catch (e: ErrorHttp) {
        when (e.codigo) {
            401 -> null
            404 -> ResultadoProblema.NoEncontrado
            else -> ResultadoProblema.ErrorInesperado
        }
    } catch (e: IOException) {
        ResultadoProblema.SinConexion
    }
}
