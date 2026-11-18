package com.mastercode.app.data.cinturon

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.conAccess
import java.io.IOException

sealed interface ResultadoCinturones {
    data class Exito(val cinturones: List<CinturonDelLenguaje>) : ResultadoCinturones
    data object SesionVencida : ResultadoCinturones
    data object SinConexion : ResultadoCinturones
    data object ErrorInesperado : ResultadoCinturones
}

// El token sale de AuthRepository, como en los demás repositorios.
class CinturonesRepository(private val api: CinturonesApi, private val auth: AuthRepository) {

    suspend fun cinturones(): ResultadoCinturones = auth.conAccess(
        vencida = ResultadoCinturones.SesionVencida,
        sinConexion = ResultadoCinturones.SinConexion,
        inesperado = ResultadoCinturones.ErrorInesperado,
    ) { access ->
        try {
            ResultadoCinturones.Exito(api.cinturones(access))
        } catch (e: ErrorHttp) {
            if (e.codigo == 401) null else ResultadoCinturones.ErrorInesperado
        } catch (e: IOException) {
            ResultadoCinturones.SinConexion
        }
    }
}
