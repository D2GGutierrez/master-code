package com.mastercode.app.ui.racha

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import com.mastercode.app.data.racha.FakeRachaApi
import com.mastercode.app.data.racha.Racha
import com.mastercode.app.data.racha.RachaRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

// HU49-T3 (CA-49.3): la racha en la pantalla principal y el mensaje que la acompaña.
@OptIn(ExperimentalCoroutinesApi::class)
class RachaViewModelTest {

    private val authApi = FakeAuthApi()
    private val api = FakeRachaApi()
    private val auth = AuthRepository(authApi, MemoriaTokenStorage(refresh = "refresh-guardado"))

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel() = RachaViewModel(RachaRepository(api, auth))

    private fun estadoCon(rachaActual: Int, practicoHoy: Boolean): EstadoDeRacha {
        api.respuesta = { Racha(rachaActual, practicoHoy, emptyList()) }
        return crearViewModel().estado.value
    }

    @Test
    fun empiezaCargandoYLuegoMuestraLaRacha() {
        val respuesta = CompletableDeferred<Racha>()
        api.respuesta = { respuesta.await() }

        val viewModel = crearViewModel()
        assertEquals(EstadoDeRacha.Cargando, viewModel.estado.value)

        respuesta.complete(Racha(4, true, emptyList()))
        assertEquals(EstadoDeRacha.ConRacha(4, true), viewModel.estado.value)
    }

    @Test
    fun sinRachaInvitaAEmpezar() {
        val estado = estadoCon(0, false) as EstadoDeRacha.ConRacha

        assertEquals(MensajeDeRacha.Empezar, estado.mensaje)
    }

    // Aunque el servidor diga que practicó hoy, con racha 0 no hay nada que celebrar: se invita a seguir.
    @Test
    fun conRachaCeroElMensajeEsEmpezarAunquePracticoHoy() {
        val estado = estadoCon(0, true) as EstadoDeRacha.ConRacha

        assertEquals(MensajeDeRacha.Empezar, estado.mensaje)
    }

    @Test
    fun conRachaYSinPracticarHoyRecuerdaPracticar() {
        val estado = estadoCon(6, false) as EstadoDeRacha.ConRacha

        assertEquals(MensajeDeRacha.PracticarHoy, estado.mensaje)
    }

    @Test
    fun conRachaYPracticandoHoyLoCelebra() {
        val estado = estadoCon(6, true) as EstadoDeRacha.ConRacha

        assertEquals(MensajeDeRacha.YaPractico, estado.mensaje)
    }

    // La racha es un adorno: cuando no se puede saber, no se muestra ni se avisa de ningún error.
    @Test
    fun sinConexionLaRachaSeOculta() {
        api.respuesta = { throw IOException("sin red") }

        assertEquals(EstadoDeRacha.Oculta, crearViewModel().estado.value)
    }

    @Test
    fun unErrorDelServidorOcultaLaRacha() {
        api.respuesta = { throw ErrorHttp(500) }

        assertEquals(EstadoDeRacha.Oculta, crearViewModel().estado.value)
    }

    @Test
    fun unaSesionVencidaOcultaLaRacha() {
        api.respuesta = { throw ErrorHttp(401) }
        authApi.respuestaRenovar = { throw ErrorHttp(401) }

        assertEquals(EstadoDeRacha.Oculta, crearViewModel().estado.value)
    }

    @Test
    fun cargarDeNuevoActualizaLaRacha() {
        api.respuesta = { Racha(1, false, emptyList()) }
        val viewModel = crearViewModel()
        assertEquals(EstadoDeRacha.ConRacha(1, false), viewModel.estado.value)

        api.respuesta = { Racha(2, true, emptyList()) }
        viewModel.cargar()

        assertEquals(EstadoDeRacha.ConRacha(2, true), viewModel.estado.value)
    }
}
