package com.mastercode.app.ui.avisos

import com.mastercode.app.data.avisos.AjustesDeAvisos
import com.mastercode.app.data.avisos.MemoriaAvisos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// HU52-T3 (CA-52.3, CA-52.4): el estudiante desactiva los avisos y elige su hora; queda guardado en el teléfono.
class AvisosViewModelTest {

    private val almacen = MemoriaAvisos()

    @Test
    fun pordefectoLosAvisosEstanActivadosALas2000() {
        val ajustes = AvisosViewModel(almacen).ajustes.value

        assertTrue(ajustes.activados)
        assertEquals(20, ajustes.hora)
        assertEquals(0, ajustes.minuto)
    }

    @Test
    fun desactivarLosAvisosQuedaGuardado() {
        val viewModel = AvisosViewModel(almacen)

        viewModel.activar(false)

        assertFalse(viewModel.ajustes.value.activados)
        assertFalse(almacen.ajustes.activados)
    }

    @Test
    fun volverAActivarlosConservaLaHoraElegida() {
        val viewModel = AvisosViewModel(almacen)
        viewModel.elegirHora(7, 30)
        viewModel.activar(false)

        viewModel.activar(true)

        assertEquals(AjustesDeAvisos(activados = true, hora = 7, minuto = 30), viewModel.ajustes.value)
    }

    @Test
    fun laHoraElegidaQuedaGuardada() {
        val viewModel = AvisosViewModel(almacen)

        viewModel.elegirHora(18, 45)

        assertEquals(AjustesDeAvisos(true, 18, 45), viewModel.ajustes.value)
        assertEquals(AjustesDeAvisos(true, 18, 45), almacen.ajustes)
    }

    @Test
    fun cambiarLaHoraNoActivaLosAvisosDesactivados() {
        val viewModel = AvisosViewModel(almacen)
        viewModel.activar(false)

        viewModel.elegirHora(9, 0)

        assertFalse(viewModel.ajustes.value.activados)
    }

    @Test
    fun lasHorasLimiteSeAceptan() {
        val viewModel = AvisosViewModel(almacen)

        viewModel.elegirHora(0, 0)
        assertEquals(AjustesDeAvisos(true, 0, 0), viewModel.ajustes.value)

        viewModel.elegirHora(23, 59)
        assertEquals(AjustesDeAvisos(true, 23, 59), viewModel.ajustes.value)
    }

    @Test
    fun unaHoraFueraDeRangoSeIgnora() {
        val viewModel = AvisosViewModel(almacen)
        viewModel.elegirHora(8, 15)

        viewModel.elegirHora(24, 0)
        viewModel.elegirHora(-1, 10)
        viewModel.elegirHora(10, 60)
        viewModel.elegirHora(10, -5)

        assertEquals(AjustesDeAvisos(true, 8, 15), viewModel.ajustes.value)
        assertEquals(1, almacen.guardados.size)
    }

    // Al volver a abrir la pantalla (o la app) se ve lo que se eligió.
    @Test
    fun unViewModelNuevoLeeLoQueSeGuardo() {
        val primero = AvisosViewModel(almacen)
        primero.activar(false)
        primero.elegirHora(6, 5)

        val segundo = AvisosViewModel(almacen)

        assertEquals(AjustesDeAvisos(false, 6, 5), segundo.ajustes.value)
    }

    @Test
    fun laHoraPorDefectoEsValida() {
        assertTrue(AjustesDeAvisos.horaValida(AjustesDeAvisos.HORA_POR_DEFECTO, AjustesDeAvisos.MINUTO_POR_DEFECTO))
        assertFalse(AjustesDeAvisos.horaValida(24, 0))
        assertFalse(AjustesDeAvisos.horaValida(0, 60))
    }

    // HU52-T4: cada cambio también se manda a user-api (en la app, MasterCodeApp.enviarAvisos).
    @Test
    fun cadaCambioSeAvisaParaMandarloAlServidor() {
        val enviados = mutableListOf<AjustesDeAvisos>()
        val viewModel = AvisosViewModel(almacen) { enviados += it }

        viewModel.activar(false)
        viewModel.elegirHora(7, 30)
        viewModel.elegirHora(99, 0)

        assertEquals(
            listOf(AjustesDeAvisos(activados = false), AjustesDeAvisos(activados = false, hora = 7, minuto = 30)),
            enviados,
        )
    }
}
