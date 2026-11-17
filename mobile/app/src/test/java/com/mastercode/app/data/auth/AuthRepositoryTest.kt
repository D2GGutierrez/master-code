package com.mastercode.app.data.auth

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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

    // El refresh rota: dos renovaciones a la vez (por ejemplo, «Mis cursos» y el reenvío de respuestas guardadas) no deben
    // usar el mismo refresh, o la segunda recibiría 401 y cerraría la sesión sin motivo.
    @Test
    fun dosRenovacionesAlMismoTiempoSeHacenUnaTrasOtraConElRefreshQueDejoLaPrimera() = runBlocking {
        val recibidos = mutableListOf<String>()
        var vueltas = 0
        val apiLenta = object : AuthApi {
            override suspend fun login(email: String, password: String): Tokens = error("no se usa")

            override suspend fun renovar(refresh: String): Tokens {
                recibidos += refresh
                delay(30)
                vueltas++
                return Tokens(access = "access-$vueltas", refresh = "refresh-$vueltas")
            }
        }
        val repositorio = AuthRepository(apiLenta, MemoriaTokenStorage(refresh = "refresh-0"))

        val a = async { repositorio.renovar() }
        val b = async { repositorio.renovar() }

        assertEquals(ResultadoRenovacion.Exito, a.await())
        assertEquals(ResultadoRenovacion.Exito, b.await())
        assertEquals(listOf("refresh-0", "refresh-1"), recibidos)
    }
}
