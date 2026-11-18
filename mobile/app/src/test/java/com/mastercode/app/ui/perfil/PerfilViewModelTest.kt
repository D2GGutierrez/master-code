package com.mastercode.app.ui.perfil

import com.mastercode.app.R
import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import com.mastercode.app.data.cinturon.CinturonDelLenguaje
import com.mastercode.app.data.cinturon.CinturonesRepository
import com.mastercode.app.data.cinturon.FakeCinturonesApi
import com.mastercode.app.data.cinturon.SiguienteCinturon
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

// HU51-T2 y HU51-T3 (CA-51.2 a CA-51.4): el perfil con el cinturón por lenguaje, la barra y el mensaje del negro.
@OptIn(ExperimentalCoroutinesApi::class)
class PerfilViewModelTest {

    private val amarillo = CinturonDelLenguaje("python", 2, "amarillo", 130, SiguienteCinturon(3, "naranja", 250), 120)
    private val negro = CinturonDelLenguaje("java", 7, "negro", 2100, null, null)

    private val authApi = FakeAuthApi()
    private val api = FakeCinturonesApi()
    private val auth = AuthRepository(authApi, MemoriaTokenStorage(refresh = "refresh-guardado"))

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel() = PerfilViewModel(CinturonesRepository(api, auth))

    @Test
    fun pasaDeCargandoALosCinturones() {
        val respuesta = CompletableDeferred<List<CinturonDelLenguaje>>()
        api.respuesta = { respuesta.await() }

        val viewModel = crearViewModel()
        assertEquals(ContenidoPerfil.Cargando, viewModel.uiState.value.contenido)

        respuesta.complete(listOf(amarillo))
        assertEquals(ContenidoPerfil.ConCinturones(listOf(amarillo)), viewModel.uiState.value.contenido)
    }

    @Test
    fun sinAciertosNoHayCinturones() {
        api.respuesta = { emptyList() }

        assertEquals(ContenidoPerfil.SinCinturones, crearViewModel().uiState.value.contenido)
    }

    // CA-51.2: un cinturón por lenguaje, en el orden del servidor.
    @Test
    fun conVariosLenguajesMuestraUnCinturonPorCadaUno() {
        api.respuesta = { listOf(amarillo, negro) }

        val contenido = crearViewModel().uiState.value.contenido as ContenidoPerfil.ConCinturones

        assertEquals(listOf("python", "java"), contenido.cinturones.map { it.lenguaje })
    }

    // CA-51.4: el que llegó al negro no tiene barra; el otro sigue con la suya.
    @Test
    fun soloElNegroCareceDeBarra() {
        api.respuesta = { listOf(amarillo, negro) }

        val contenido = crearViewModel().uiState.value.contenido as ContenidoPerfil.ConCinturones

        assertEquals(listOf(false, true), contenido.cinturones.map { it.esElMaximo })
        assertEquals(listOf(true, false), contenido.cinturones.map { it.avance != null })
    }

    @Test
    fun sinConexionMuestraErrorYReintentarCargaLosCinturones() {
        api.respuesta = { throw IOException("sin red") }
        val viewModel = crearViewModel()
        assertEquals(ContenidoPerfil.Error(R.string.perfil_error_conexion), viewModel.uiState.value.contenido)

        api.respuesta = { listOf(amarillo) }
        viewModel.cargar()

        assertEquals(ContenidoPerfil.ConCinturones(listOf(amarillo)), viewModel.uiState.value.contenido)
    }

    @Test
    fun unErrorDelServidorMuestraElMensajeInesperado() {
        api.respuesta = { throw ErrorHttp(500) }

        assertEquals(ContenidoPerfil.Error(R.string.perfil_error_inesperado), crearViewModel().uiState.value.contenido)
    }

    @Test
    fun un401QueNoSePuedeRenovarMuestraSesionVencida() {
        api.respuesta = { throw ErrorHttp(401) }
        authApi.respuestaRenovar = { throw ErrorHttp(401) }

        assertEquals(ContenidoPerfil.SesionVencida, crearViewModel().uiState.value.contenido)
        assertTrue(!auth.haySesionGuardada())
    }

    @Test
    fun reintentarVuelveACargandoMientrasEspera() {
        api.respuesta = { throw IOException("sin red") }
        val viewModel = crearViewModel()
        val respuesta = CompletableDeferred<List<CinturonDelLenguaje>>()
        api.respuesta = { respuesta.await() }

        viewModel.cargar()
        assertEquals(ContenidoPerfil.Cargando, viewModel.uiState.value.contenido)

        respuesta.complete(emptyList())
        assertEquals(ContenidoPerfil.SinCinturones, viewModel.uiState.value.contenido)
    }
}
