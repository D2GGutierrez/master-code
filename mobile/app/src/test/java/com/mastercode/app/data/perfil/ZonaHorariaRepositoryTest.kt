package com.mastercode.app.data.perfil

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

// H6: la app manda la zona del teléfono con la misma renovación de token que las demás llamadas, y un fallo no rompe nada.
class ZonaHorariaRepositoryTest {

    private class FakeZonaHorariaApi : ZonaHorariaApi {
        var alGuardar: suspend (access: String, zona: String) -> Unit = { _, _ -> }
        val accessRecibidos = mutableListOf<String>()
        val zonasRecibidas = mutableListOf<String>()

        override suspend fun guardar(access: String, zonaHoraria: String) {
            accessRecibidos += access
            zonasRecibidas += zonaHoraria
            alGuardar(access, zonaHoraria)
        }
    }

    private val api = FakeZonaHorariaApi()
    private val authApi = FakeAuthApi()
    private val auth = AuthRepository(authApi, MemoriaTokenStorage(refresh = "refresh-guardado"))
    private val repository = ZonaHorariaRepository(api, auth) { "America/Lima" }

    @Test
    fun alAbrirLaAppSinAccessEnMemoriaRenuevaConElRefreshYManda() = runTest {
        val resultado = repository.enviar()

        assertEquals(ResultadoZonaHoraria.Enviada, resultado)
        assertEquals(listOf("access-2"), api.accessRecibidos)
        assertEquals(listOf("America/Lima"), api.zonasRecibidas)
    }

    @Test
    fun conElAccessEnMemoriaLoUsaSinRenovar() = runTest {
        auth.renovar()
        api.accessRecibidos.clear()
        authApi.respuestaRenovar = { throw IOException("no debería renovar") }

        val resultado = repository.enviar()

        assertEquals(ResultadoZonaHoraria.Enviada, resultado)
        assertEquals(listOf("access-2"), api.accessRecibidos)
    }

    @Test
    fun siElAccessVencioRenuevaYPideUnaSolaVezMas() = runTest {
        auth.renovar()
        api.accessRecibidos.clear()
        authApi.respuestaRenovar = { com.mastercode.app.data.auth.Tokens("access-3", "refresh-3") }
        var primera = true
        api.alGuardar = { _, _ ->
            if (primera) {
                primera = false
                throw ErrorHttp(401)
            }
        }

        val resultado = repository.enviar()

        assertEquals(ResultadoZonaHoraria.Enviada, resultado)
        assertEquals(listOf("access-2", "access-3"), api.accessRecibidos)
    }

    @Test
    fun conLaSesionExpiradaNoMandaNada() = runTest {
        authApi.respuestaRenovar = { throw ErrorHttp(401) }

        assertEquals(ResultadoZonaHoraria.SesionVencida, repository.enviar())
        assertEquals(emptyList<String>(), api.zonasRecibidas)
    }

    @Test
    fun sinConexionSeDiceYNoHayExcepcion() = runTest {
        api.alGuardar = { _, _ -> throw IOException("sin red") }

        assertEquals(ResultadoZonaHoraria.SinConexion, repository.enviar())
    }

    @Test
    fun siUserApiRechazaLaZonaEsUnErrorInesperadoYNoSeReintentaEnBucle() = runTest {
        api.alGuardar = { _, _ -> throw ErrorHttp(400) }

        assertEquals(ResultadoZonaHoraria.ErrorInesperado, repository.enviar())
        assertEquals(1, api.zonasRecibidas.size)
    }

    @Test
    fun unErrorDelServidorEsInesperado() = runTest {
        api.alGuardar = { _, _ -> throw ErrorHttp(500) }

        assertEquals(ResultadoZonaHoraria.ErrorInesperado, repository.enviar())
    }

    @Test
    fun laZonaSeLeeEnCadaEnvioPorqueElEstudiantePuedeViajar() = runTest {
        var zona = "America/Lima"
        val viajero = ZonaHorariaRepository(api, auth) { zona }

        viajero.enviar()
        zona = "Europe/Madrid"
        viajero.enviar()

        assertEquals(listOf("America/Lima", "Europe/Madrid"), api.zonasRecibidas)
    }

    @Test
    fun porDefectoUsaLaZonaDeLaJvmQueEnElTelefonoEsLaDelSistema() = runTest {
        val real = ZonaHorariaRepository(api, auth)

        real.enviar()

        assertEquals(java.util.TimeZone.getDefault().id, api.zonasRecibidas.single())
    }
}
