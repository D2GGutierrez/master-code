package com.mastercode.app.data.racha

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.conAccess
import java.io.IOException

sealed interface ResultadoRacha {
    data class Exito(val racha: Racha) : ResultadoRacha
    data object SesionVencida : ResultadoRacha
    data object SinConexion : ResultadoRacha
    data object ErrorInesperado : ResultadoRacha
}

// El token sale de AuthRepository, como en los demás repositorios.
class RachaRepository(private val api: RachaApi, private val auth: AuthRepository) {

    suspend fun racha(): ResultadoRacha = auth.conAccess(
        vencida = ResultadoRacha.SesionVencida,
        sinConexion = ResultadoRacha.SinConexion,
        inesperado = ResultadoRacha.ErrorInesperado,
    ) { access ->
        try {
            ResultadoRacha.Exito(api.racha(access))
        } catch (e: ErrorHttp) {
            if (e.codigo == 401) null else ResultadoRacha.ErrorInesperado
        } catch (e: IOException) {
            ResultadoRacha.SinConexion
        }
    }
}
