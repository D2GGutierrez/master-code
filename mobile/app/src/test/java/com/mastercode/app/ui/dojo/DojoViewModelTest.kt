package com.mastercode.app.ui.dojo

import com.mastercode.app.R
import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import com.mastercode.app.data.dojo.DojoRepository
import com.mastercode.app.data.dojo.FakeDojoApi
import com.mastercode.app.data.problemas.ProblemaDelEjercicio
import com.mastercode.app.data.respuestas.EstadoDeSesion
import com.mastercode.app.data.respuestas.MemoriaSesion
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

// HU53-T3 (CA-53.1, CA-53.4) y HU55-T1 (CA-55.1, CA-55.5): la pantalla del Dojo abre la sesión, muestra el contador de
// aciertos y guarda el estado de la sesión.
@OptIn(ExperimentalCoroutinesApi::class)
class DojoViewModelTest {

    private val primero = ProblemaDelEjercicio(12, "Suma", "¿Qué imprime?", "prediccion_salida", intentosRestantes = 3)
    private val segundo = ProblemaDelEjercicio(13, "Resta", "¿Qué imprime?", "prediccion_salida", intentosRestantes = 3)

    private val api = FakeDojoApi()
    private val almacen = MemoriaSesion()
    private val auth = AuthRepository(FakeAuthApi(), MemoriaTokenStorage(refresh = "refresh-guardado"))

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crear() = DojoViewModel(DojoRepository(api, auth), almacen)

    @Test
    fun empiezaPidiendoElLenguaje() {
        val viewModel = crear()

        assertEquals(FaseDelDojo.Eligiendo, viewModel.uiState.value.fase)
        assertEquals(0, viewModel.uiState.value.aciertos)
        assertEquals(emptyList<String>(), api.lenguajesPedidos)
    }

    // CA-53.1: al elegir el lenguaje se inicia la sesión y se muestra el primer ejercicio.
    @Test
    fun alElegirElLenguajeIniciaLaSesionYMuestraElPrimerEjercicio() {
        api.ejercicios = mutableListOf(primero)
        val viewModel = crear()

        viewModel.elegirLenguaje("python")

        val estado = viewModel.uiState.value
        assertEquals(listOf("python"), api.lenguajesPedidos)
        assertEquals(listOf(5L), api.sesionesPedidas)
        assertEquals(5L, estado.sesionId)
        assertEquals(FaseDelDojo.Ejercicio(primero), estado.fase)
        assertEquals(0, estado.aciertos)
        assertEquals(false, estado.respondido)
    }

    @Test
    fun unLenguajeQueNoEsDelDojoSeIgnora() {
        val viewModel = crear()

        viewModel.elegirLenguaje("cobol")

        assertEquals(FaseDelDojo.Eligiendo, viewModel.uiState.value.fase)
        assertEquals(emptyList<String>(), api.lenguajesPedidos)
    }

    // CA-53.4: el contador sube con cada acierto y no con los errores.
    @Test
    fun elContadorDeAciertosSubeSoloConLosAciertos() {
        api.ejercicios = mutableListOf(primero, segundo)
        val viewModel = crear()
        viewModel.elegirLenguaje("python")

        viewModel.alResponder(12, acerto = true)
        assertEquals(1, viewModel.uiState.value.aciertos)
        assertEquals(true, viewModel.uiState.value.respondido)

        viewModel.siguiente()
        assertEquals(FaseDelDojo.Ejercicio(segundo), viewModel.uiState.value.fase)
        assertEquals(false, viewModel.uiState.value.respondido)

        viewModel.alResponder(13, acerto = false)
        assertEquals(1, viewModel.uiState.value.aciertos)
        assertEquals(2, viewModel.uiState.value.respondidos)
    }

    // La pantalla puede avisar otra vez de la misma respuesta (por ejemplo al girar el teléfono): no suma dos veces.
    @Test
    fun avisarDosVecesDeLaMismaRespuestaNoLaCuentaDosVeces() {
        api.ejercicios = mutableListOf(primero)
        val viewModel = crear()
        viewModel.elegirLenguaje("python")

        viewModel.alResponder(12, acerto = true)
        viewModel.alResponder(12, acerto = true)

        assertEquals(1, viewModel.uiState.value.aciertos)
        assertEquals(1, viewModel.uiState.value.respondidos)
    }

    @Test
    fun noSePasaAlSiguienteSinRespuestaCorregida() {
        api.ejercicios = mutableListOf(primero, segundo)
        val viewModel = crear()
        viewModel.elegirLenguaje("python")

        viewModel.siguiente()

        assertEquals(FaseDelDojo.Ejercicio(primero), viewModel.uiState.value.fase)
        assertEquals(listOf(5L), api.sesionesPedidas)
    }

    @Test
    fun unEjercicioQueSeOmiteDejaPasarAlSiguienteSinContarse() {
        api.ejercicios = mutableListOf(primero, segundo)
        val viewModel = crear()
        viewModel.elegirLenguaje("python")

        viewModel.omitir()
        viewModel.siguiente()

        assertEquals(FaseDelDojo.Ejercicio(segundo), viewModel.uiState.value.fase)
        assertEquals(0, viewModel.uiState.value.respondidos)
    }

    @Test
    fun cuandoNoQuedanEjerciciosLoDice() {
        api.ejercicios = mutableListOf(primero)
        val viewModel = crear()
        viewModel.elegirLenguaje("python")
        viewModel.alResponder(12, acerto = true)

        viewModel.siguiente()

        assertEquals(FaseDelDojo.SinMasEjercicios, viewModel.uiState.value.fase)
        assertEquals(1, viewModel.uiState.value.aciertos)
    }

    // HU55-T1 (CA-55.1): el estado de la sesión queda guardado con el ejercicio actual y los aciertos.
    @Test
    fun guardaLaSesionConElEjercicioActualYLosAciertos() {
        api.ejercicios = mutableListOf(primero, segundo)
        val viewModel = crear()

        viewModel.elegirLenguaje("python")
        assertEquals(EstadoDeSesion(5, "python", 12, 0, 0), almacen.estado)

        viewModel.alResponder(12, acerto = true)
        assertEquals(EstadoDeSesion(5, "python", 12, 1, 1), almacen.estado)

        viewModel.siguiente()
        assertEquals(EstadoDeSesion(5, "python", 13, 1, 1), almacen.estado)
    }

    // HU55-T1 (CA-55.5): al salir de la pantalla el estado se guarda otra vez, también si se estaba cargando.
    @Test
    fun alSalirDeLaPantallaGuardaElEstado() {
        api.ejercicios = mutableListOf(primero)
        val viewModel = crear()
        viewModel.elegirLenguaje("python")
        almacen.guardar(null)

        viewModel.alSalir()

        assertEquals(EstadoDeSesion(5, "python", 12, 0, 0), almacen.estado)
    }

    @Test
    fun alSalirSinHaberIniciadoNoGuardaNada() {
        val viewModel = crear()

        viewModel.alSalir()

        assertNull(almacen.estado)
        assertEquals(emptyList<EstadoDeSesion?>(), almacen.guardados)
    }

    @Test
    fun alSalirMientrasCargaElSiguienteGuardaElUltimoEjercicioConocido() {
        val espera = CompletableDeferred<Unit>()
        api.ejercicios = mutableListOf(primero, segundo)
        val viewModel = crear()
        viewModel.elegirLenguaje("python")
        viewModel.alResponder(12, acerto = true)
        api.alPedirSiguiente = { espera.await() }
        viewModel.siguiente()
        assertEquals(FaseDelDojo.CargandoEjercicio, viewModel.uiState.value.fase)

        viewModel.alSalir()

        assertEquals(EstadoDeSesion(5, "python", 12, 1, 1), almacen.estado)
    }

    @Test
    fun sinConexionAlIniciarMuestraElErrorYReintentarVuelveAElegir() {
        api.alIniciar = { throw IOException("sin red") }
        val viewModel = crear()

        viewModel.elegirLenguaje("python")
        assertEquals(FaseDelDojo.Error(R.string.dojo_error_conexion), viewModel.uiState.value.fase)

        viewModel.alReintentar()
        assertEquals(FaseDelDojo.Eligiendo, viewModel.uiState.value.fase)
    }

    @Test
    fun sinConexionAlPedirElSiguienteSePuedeReintentarSinPerderLosAciertos() {
        api.ejercicios = mutableListOf(primero, segundo)
        val viewModel = crear()
        viewModel.elegirLenguaje("python")
        viewModel.alResponder(12, acerto = true)
        api.alPedirSiguiente = { throw IOException("sin red") }
        viewModel.siguiente()
        assertEquals(FaseDelDojo.Error(R.string.dojo_error_conexion), viewModel.uiState.value.fase)

        api.alPedirSiguiente = null
        viewModel.alReintentar()

        assertEquals(FaseDelDojo.Ejercicio(segundo), viewModel.uiState.value.fase)
        assertEquals(1, viewModel.uiState.value.aciertos)
    }

    // Una sesión que ya terminó en el servidor no se puede retomar: se borra lo guardado.
    @Test
    fun siLaSesionYaTerminoSeBorraLoGuardado() {
        val viewModel = crear()
        api.ejercicios = mutableListOf(primero)
        viewModel.elegirLenguaje("python")
        viewModel.alResponder(12, acerto = true)
        api.alPedirSiguiente = { throw ErrorHttp(409) }

        viewModel.siguiente()

        assertEquals(FaseDelDojo.Error(R.string.dojo_error_sesion), viewModel.uiState.value.fase)
        assertNull(almacen.estado)
        assertNull(viewModel.uiState.value.sesionId)
    }

    @Test
    fun unaSesionVencidaPideIniciarSesion() {
        api.alIniciar = { throw ErrorHttp(401) }
        val auth401 = FakeAuthApi().apply { respuestaRenovar = { throw ErrorHttp(401) } }
        val viewModel = DojoViewModel(
            DojoRepository(api, AuthRepository(auth401, MemoriaTokenStorage(refresh = "vencido"))),
            almacen,
        )

        viewModel.elegirLenguaje("python")

        assertEquals(FaseDelDojo.SesionVencida, viewModel.uiState.value.fase)
    }
}
