package com.mastercode.app.ui.cinturon

import com.mastercode.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// HU51-T2: el nombre en español de cada nivel de cinturón (la tabla de niveles va de 1 a 7).
class NombreDeCinturonTest {

    @Test
    fun cadaNivelTieneSuNombre() {
        assertEquals(R.string.cinturon_blanco, nombreDeCinturon(1))
        assertEquals(R.string.cinturon_amarillo, nombreDeCinturon(2))
        assertEquals(R.string.cinturon_naranja, nombreDeCinturon(3))
        assertEquals(R.string.cinturon_verde, nombreDeCinturon(4))
        assertEquals(R.string.cinturon_azul, nombreDeCinturon(5))
        assertEquals(R.string.cinturon_marron, nombreDeCinturon(6))
        assertEquals(R.string.cinturon_negro, nombreDeCinturon(7))
    }

    // Un nivel que la app no conoce no revienta: la pantalla usa el nombre que mande el servidor.
    @Test
    fun unNivelDesconocidoNoTieneNombre() {
        assertNull(nombreDeCinturon(0))
        assertNull(nombreDeCinturon(8))
        assertNull(nombreDeCinturon(-1))
    }
}
