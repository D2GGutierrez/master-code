package com.mastercode.app.ui.componentes

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatoDeTiempoTest {
    @Test
    fun cerosAlPrincipio() {
        assertEquals("00:00", formatoDeTiempo(0))
        assertEquals("00:09", formatoDeTiempo(9))
    }

    @Test
    fun pasaDeSegundosAMinutos() {
        assertEquals("00:59", formatoDeTiempo(59))
        assertEquals("01:00", formatoDeTiempo(60))
        assertEquals("02:05", formatoDeTiempo(125))
    }

    @Test
    fun masDeUnaHoraSigueContandoMinutos() {
        assertEquals("61:01", formatoDeTiempo(3661))
    }

    @Test
    fun unTiempoNegativoCuentaComoCero() {
        assertEquals("00:00", formatoDeTiempo(-5))
    }
}
