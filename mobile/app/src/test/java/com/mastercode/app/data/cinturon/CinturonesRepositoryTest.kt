package com.mastercode.app.data.cinturon

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import com.mastercode.app.data.auth.Tokens
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

// HU51-T2: el repositorio de los cinturones entrega el resultado que el perfil necesita y renueva el access si venció.
class CinturonesRepositoryTest {

    private val cinturones = listOf(
        CinturonDelLenguaje("python", 2, "amarillo", 130, SiguienteCinturon(3, "naranja", 250), 120),
    )
    private val authApi = FakeAuthApi()
    private val api = FakeCinturonesApi()
    private val auth = AuthRepository(authApi, MemoriaTokenStorage(refresh = "refresh-guardado"))

    private fun repositorio() = CinturonesRepository(api, auth)

    @Test
    fun entregaLosCinturonesDelServidor() = runTest {
        api.respuesta = { cinturones }

        assertEquals(ResultadoCinturones.Exito(cinturones), repositorio().cinturones())
    }

    @Test
    fun unAccessVencidoSeRenuevaYSeReintentaUnaVez() = runTest {
        api.respuesta = { access -> if (access == "access-viejo") throw ErrorHttp(401) else cinturones }
        authApi.respuestaLogin = { Tokens(access = "access-viejo", refresh = "refresh-1") }
        auth.login("ana@ejemplo.edu.pe", "clave-inventada")

        val resultado = repositorio().cinturones()

        assertEquals(ResultadoCinturones.Exito(cinturones), resultado)
        assertEquals(listOf("access-viejo", "access-2"), api.accessRecibidos)
    }

    @Test
    fun siNoSePuedeRenovarLaSesionEstaVencida() = runTest {
        api.respuesta = { throw ErrorHttp(401) }
        authApi.respuestaRenovar = { throw ErrorHttp(401) }

        assertEquals(ResultadoCinturones.SesionVencida, repositorio().cinturones())
    }

    @Test
    fun sinConexionSeAvisa() = runTest {
        api.respuesta = { throw IOException("sin red") }

        assertEquals(ResultadoCinturones.SinConexion, repositorio().cinturones())
    }

    @Test
    fun unErrorDelServidorEsInesperado() = runTest {
        api.respuesta = { throw ErrorHttp(500) }

        assertEquals(ResultadoCinturones.ErrorInesperado, repositorio().cinturones())
    }
}
