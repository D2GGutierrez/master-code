package com.mastercode.app.data.racha

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import com.mastercode.app.data.auth.Tokens
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

// HU49-T3: el repositorio de la racha entrega el resultado que la pantalla necesita y renueva el access si venció.
class RachaRepositoryTest {

    private val racha = Racha(3, true, listOf(RachaDeLenguaje("python", 3, 4, true)))
    private val authApi = FakeAuthApi()
    private val api = FakeRachaApi()
    private val auth = AuthRepository(authApi, MemoriaTokenStorage(refresh = "refresh-guardado"))

    private fun repositorio() = RachaRepository(api, auth)

    @Test
    fun entregaLaRachaDelServidor() = runTest {
        api.respuesta = { racha }

        assertEquals(ResultadoRacha.Exito(racha), repositorio().racha())
    }

    @Test
    fun unAccessVencidoSeRenuevaYSeReintentaUnaVez() = runTest {
        api.respuesta = { access -> if (access == "access-viejo") throw ErrorHttp(401) else racha }
        authApi.respuestaLogin = { Tokens(access = "access-viejo", refresh = "refresh-1") }
        auth.login("ana@ejemplo.edu.pe", "clave-inventada")

        val resultado = repositorio().racha()

        assertEquals(ResultadoRacha.Exito(racha), resultado)
        assertEquals(listOf("access-viejo", "access-2"), api.accessRecibidos)
    }

    @Test
    fun siNoSePuedeRenovarLaSesionEstaVencida() = runTest {
        api.respuesta = { throw ErrorHttp(401) }
        authApi.respuestaRenovar = { throw ErrorHttp(401) }

        assertEquals(ResultadoRacha.SesionVencida, repositorio().racha())
    }

    @Test
    fun sinConexionSeAvisa() = runTest {
        api.respuesta = { throw IOException("sin red") }

        assertEquals(ResultadoRacha.SinConexion, repositorio().racha())
    }

    @Test
    fun unErrorDelServidorEsInesperado() = runTest {
        api.respuesta = { throw ErrorHttp(500) }

        assertEquals(ResultadoRacha.ErrorInesperado, repositorio().racha())
    }
}
