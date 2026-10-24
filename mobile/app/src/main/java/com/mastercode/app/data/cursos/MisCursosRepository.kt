package com.mastercode.app.data.cursos

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.ResultadoRenovacion
import java.io.IOException

sealed interface ResultadoMisCursos {
    data class Exito(val cursos: List<CursoInscrito>) : ResultadoMisCursos
    data object SesionVencida : ResultadoMisCursos
    data object SinConexion : ResultadoMisCursos
    data object ErrorInesperado : ResultadoMisCursos
}

// El token sale de AuthRepository, el mismo que usa el login: no hay un segundo lugar con tokens.
class MisCursosRepository(private val api: MisCursosApi, private val auth: AuthRepository) {

    suspend fun misCursos(): ResultadoMisCursos {
        // Al abrir la app el access no está en memoria; si está pero venció (15 minutos), user-api da 401.
        // En los dos casos se renueva con el refresh guardado y se pide una sola vez más.
        auth.accessToken?.let { access -> pedir(access)?.let { return it } }
        return when (auth.renovar()) {
            // Un 401 con un access recién emitido no es una sesión vencida: es otro problema.
            ResultadoRenovacion.Exito -> auth.accessToken?.let { pedir(it) } ?: ResultadoMisCursos.ErrorInesperado
            ResultadoRenovacion.SesionExpirada -> ResultadoMisCursos.SesionVencida
            ResultadoRenovacion.SinConexion -> ResultadoMisCursos.SinConexion
            ResultadoRenovacion.ErrorInesperado -> ResultadoMisCursos.ErrorInesperado
        }
    }

    // null significa 401: el access ya no sirve.
    private suspend fun pedir(access: String): ResultadoMisCursos? = try {
        ResultadoMisCursos.Exito(api.misCursos(access))
    } catch (e: ErrorHttp) {
        if (e.codigo == 401) null else ResultadoMisCursos.ErrorInesperado
    } catch (e: IOException) {
        ResultadoMisCursos.SinConexion
    }
}
