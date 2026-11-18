package com.mastercode.app.data.cinturon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// HU51-T2 y HU51-T3: cuánto se llena la barra y cuándo no hay barra (el negro). Los umbrales son los de
// docs/puntos-cinturon.md: 0, 100, 250, 500, 900, 1400 y 2000.
class CinturonDelLenguajeTest {

    private val umbrales = listOf(0, 100, 250, 500, 900, 1400, 2000)
    private val nombres = listOf("blanco", "amarillo", "naranja", "verde", "azul", "marron", "negro")

    // El cinturón que tiene un estudiante con «puntos» acumulados, como lo arma user-api.
    private fun conPuntos(puntos: Int): CinturonDelLenguaje {
        val i = umbrales.indexOfLast { puntos >= it }
        val siguiente = if (i < umbrales.size - 1) SiguienteCinturon(i + 2, nombres[i + 1], umbrales[i + 1]) else null
        return CinturonDelLenguaje("python", i + 1, nombres[i], puntos, siguiente, siguiente?.let { it.puntosRequeridos - puntos })
    }

    @Test
    fun conLaMitadDeLosPuntosDelSiguienteLaBarraVaALaMitad() {
        assertEquals(0.5f, conPuntos(50).avance!!, 0.0001f)
        assertEquals(0.5f, conPuntos(125).avance!!, 0.0001f)
    }

    @Test
    fun conPocosPuntosLaBarraVaCasiVacia() {
        assertEquals(0.1f, conPuntos(10).avance!!, 0.0001f)
    }

    // Un punto antes del umbral la barra casi se llena, sin pasarse de 1.
    @Test
    fun unPuntoAntesDelSiguienteLaBarraEstaCasiLlena() {
        for (i in 1 until umbrales.size) {
            val avance = conPuntos(umbrales[i] - 1).avance!!

            assertTrue("antes de ${umbrales[i]}: $avance", avance < 1f && avance > 0.9f)
        }
    }

    // Justo en el umbral ya es el cinturón nuevo, así que la barra apunta al que sigue.
    @Test
    fun enElUmbralLaBarraSeMideContraElSiguienteCinturon() {
        val c = conPuntos(100)

        assertEquals("amarillo", c.nombre)
        assertEquals(0.4f, c.avance!!, 0.0001f)
    }

    @Test
    fun elAvanceNuncaSalaDeCeroAUno() {
        for (puntos in 1..1999) {
            val avance = conPuntos(puntos).avance!!

            assertTrue("con $puntos puntos: $avance", avance in 0f..1f)
        }
    }

    @Test
    fun unAvanceFueraDeRangoSeAcota() {
        val demasiado = CinturonDelLenguaje("python", 1, "blanco", 500, SiguienteCinturon(2, "amarillo", 100), -400)
        val negativo = CinturonDelLenguaje("python", 1, "blanco", -5, SiguienteCinturon(2, "amarillo", 100), 105)

        assertEquals(1f, demasiado.avance!!, 0f)
        assertEquals(0f, negativo.avance!!, 0f)
    }

    // El servidor no debería mandar un umbral de 0, pero si llega no se divide entre cero.
    @Test
    fun unSiguienteSinUmbralLlenaLaBarra() {
        val c = CinturonDelLenguaje("python", 1, "blanco", 0, SiguienteCinturon(2, "amarillo", 0), 0)

        assertEquals(1f, c.avance!!, 0f)
    }

    // CA-51.4: el negro no tiene barra; tiene mensaje especial.
    @Test
    fun elNegroNoTieneBarraYEsElMaximo() {
        for (puntos in listOf(2000, 2001, 9999)) {
            val negro = conPuntos(puntos)

            assertEquals("negro", negro.nombre)
            assertTrue(negro.esElMaximo)
            assertNull(negro.avance)
        }
    }

    @Test
    fun losDemasCinturonesNoSonElMaximo() {
        for (puntos in listOf(10, 100, 250, 500, 900, 1400, 1999)) {
            assertFalse("con $puntos puntos", conPuntos(puntos).esElMaximo)
        }
    }
}
