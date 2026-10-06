package com.mastercode.app.data.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class AuthRepositoryTest {
    private val api = FakeAuthApi()
    private val storage = MemoriaTokenStorage()
    private val repository = AuthRepository(api, storage)

    @Test
    fun loginCorrectoGuardaElRefreshYDejaElAccessEnMemoria() = runTest {
        val resultado = repository.login("ana@tecsup.edu.pe", "clave-segura")

        assertEquals(ResultadoLogin.Exito, resultado)
        assertEquals("refresh-1", storage.refresh)
        assertEquals("access-1", repository.accessToken)
    }

    @Test
    fun credencialesIncorrectasNoGuardanNada() = runTest {
        api.respuestaLogin = { throw ErrorHttp(401) }

        val resultado = repository.login("ana@tecsup.edu.pe", "mala")

        assertEquals(ResultadoLogin.CredencialesIncorrectas, resultado)
        assertNull(storage.refresh)
        assertNull(repository.accessToken)
    }

    @Test
    fun cuentaInactivaDevuelvePendienteDeActivacion() = runTest {
        api.respuestaLogin = { throw ErrorHttp(403) }

        assertEquals(ResultadoLogin.PendienteActivacion, repository.login("ana@tecsup.edu.pe", "clave"))
        assertNull(storage.refresh)
    }

    @Test
    fun sinConexionDevuelveSinConexion() = runTest {
        api.respuestaLogin = { throw IOException() }

        assertEquals(ResultadoLogin.SinConexion, repository.login("ana@tecsup.edu.pe", "clave"))
        assertNull(storage.refresh)
    }

    @Test
    fun renovarReemplazaElRefreshGuardado() = runTest {
        storage.refresh = "refresh-viejo"

        val resultado = repository.renovar()

        assertEquals(ResultadoRenovacion.Exito, resultado)
        assertEquals("refresh-viejo", api.refreshRecibido)
        assertEquals("refresh-2", storage.refresh)
        assertEquals("access-2", repository.accessToken)
    }

    @Test
    fun refreshVencidoSeBorraYDevuelveSesionExpirada() = runTest {
        storage.refresh = "refresh-vencido"
        api.respuestaRenovar = { throw ErrorHttp(401) }

        assertEquals(ResultadoRenovacion.SesionExpirada, repository.renovar())
        assertNull(storage.refresh)
    }

    @Test
    fun renovarSinConexionConservaElRefresh() = runTest {
        storage.refresh = "refresh-vigente"
        api.respuestaRenovar = { throw IOException() }

        assertEquals(ResultadoRenovacion.SinConexion, repository.renovar())
        assertEquals("refresh-vigente", storage.refresh)
    }
}
