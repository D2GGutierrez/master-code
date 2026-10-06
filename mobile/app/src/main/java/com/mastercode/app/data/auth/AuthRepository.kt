package com.mastercode.app.data.auth

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

    suspend fun renovar(): ResultadoRenovacion {
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
