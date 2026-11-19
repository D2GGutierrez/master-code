package com.mastercode.app.data.duelos

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

// HU57-T3, H8, HU59-T3 y HU60-T3: el repositorio traduce cada fallo de user-api a un resultado que las pantallas entienden.
class DuelosRepositoryTest {

    private val api = FakeDuelosApi()
    private val authApi = FakeAuthApi()
    private val auth = AuthRepository(authApi, MemoriaTokenStorage(refresh = "refresh-guardado"))

    private fun repositorio() = DuelosRepository(api, auth)

    @Test
    fun unaLlamadaQueSalioBienDaElValor() = runTest {
        api.alPedirCompaneros = { listOf(Companero(4, "beto")) }

        val resultado = repositorio().companeros(3)

        assertEquals(ResultadoDuelo.Exito(listOf(Companero(4, "beto"))), resultado)
        assertEquals(listOf(3L), api.companerosPedidos)
    }

    @Test
    fun loQueUserApiRechazaLlegaConSuCodigo() = runTest {
        for (codigo in listOf(400, 403, 404, 409, 503)) {
            api.alRetar = { _, _ -> throw ErrorHttp(codigo) }

            assertEquals(ResultadoDuelo.Rechazado(codigo), repositorio().retar(3, 4))
        }
    }

    @Test
    fun otroErrorDelServidorEsInesperado() = runTest {
        api.alPedirInvitaciones = { throw ErrorHttp(500) }

        assertEquals(ResultadoDuelo.ErrorInesperado, repositorio().invitaciones())
    }

    @Test
    fun sinSenalEsSinConexion() = runTest {
        api.alPedirMarcador = { throw IOException("sin red") }

        assertEquals(ResultadoDuelo.SinConexion, repositorio().marcador(7, null))
    }

    // Un 401 renueva el access una vez y repite la llamada.
    @Test
    fun conElAccessVencidoRenuevaYRepite() = runTest {
        auth.renovar()
        var intentos = 0
        api.alPedirHistorial = { pagina, tamano ->
            intentos++
            if (intentos == 1) throw ErrorHttp(401) else PaginaDeHistorial(emptyList(), pagina, tamano, 0)
        }

        val resultado = repositorio().historial(1, 20)

        assertEquals(2, intentos)
        assertEquals(ResultadoDuelo.Exito(PaginaDeHistorial(emptyList(), 1, 20, 0)), resultado)
    }

    @Test
    fun sinSesionValidaEsSesionVencida() = runTest {
        val sinRefresh = AuthRepository(authApi, MemoriaTokenStorage(refresh = null))

        assertEquals(ResultadoDuelo.SesionVencida, DuelosRepository(api, sinRefresh).ranking(3, 1, 20))
    }

    @Test
    fun elMarcadorSinCambiosLlegaSinMarcadorNuevo() = runTest {
        val primera = repositorio().marcador(7, null) as ResultadoDuelo.Exito
        val segunda = repositorio().marcador(7, primera.valor.etag) as ResultadoDuelo.Exito

        assertEquals(null, segunda.valor.marcador)
        assertEquals(listOf(null, "\"1\""), api.etagsEnviados)
    }
}
