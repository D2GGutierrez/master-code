package com.mastercode.app.ui.duelos

import com.mastercode.app.R
import com.mastercode.app.data.duelos.FakeDuelosApi.Companion.marcadorDe
import com.mastercode.app.data.duelos.PartidaDelHistorial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

// HU60-T3 y HU59-T3: los textos de historial, ranking y final de partida que salen de lo que manda user-api.
class EtiquetasDeDueloTest {

    private fun partida(estado: String, resultado: String? = null) =
        PartidaDelHistorial(1, 3, estado, "beto", 3, 1, resultado, null, null)

    @Test
    fun laEtiquetaDelHistorialDiceQuienGanoOComoEstaLaPartida() {
        assertEquals(R.string.historial_gane, etiquetaDeLaPartida(partida("terminada", "gane")))
        assertEquals(R.string.historial_perdi, etiquetaDeLaPartida(partida("terminada", "perdi")))
        assertEquals(R.string.historial_empate, etiquetaDeLaPartida(partida("terminada", "empate")))
        assertEquals(R.string.historial_en_curso, etiquetaDeLaPartida(partida("en_curso")))
        assertEquals(R.string.historial_pendiente, etiquetaDeLaPartida(partida("pendiente")))
        assertEquals(R.string.historial_rechazada, etiquetaDeLaPartida(partida("rechazada")))
        assertEquals(R.string.historial_caducada, etiquetaDeLaPartida(partida("caducada")))
    }

    @Test
    fun soloUnaPartidaEnCursoSePuedeContinuar() {
        assertTrue(sePuedeContinuar(partida("en_curso")))
        assertFalse(sePuedeContinuar(partida("terminada", "gane")))
        assertFalse(sePuedeContinuar(partida("pendiente")))
    }

    @Test
    fun laFechaSeMuestraEnLaZonaDelTelefono() {
        assertEquals("22/11/2026", fechaCorta("2026-11-22T10:00:00Z", ZoneId.of("UTC")))
        // 02:00 UTC del 22 son las 21:00 del 21 en Lima (UTC-5).
        assertEquals("21/11/2026", fechaCorta("2026-11-22T02:00:00Z", ZoneId.of("America/Lima")))
        assertNull(fechaCorta(null))
        assertEquals("rara", fechaCorta("rara"))
    }

    @Test
    fun elPorcentajeSeRedondea() {
        assertEquals("75 %", porcentajeTexto(75.0))
        assertEquals("33 %", porcentajeTexto(33.3333))
        assertEquals("0 %", porcentajeTexto(0.0))
    }

    @Test
    fun elFinalDiceQuienGanoYElMotivoSoloSiFueAbandono() {
        val gane = textoFinalDe(marcadorDe(7, estado = "terminada", resultado = "gane", motivoFin = "completada"))
        assertEquals(R.string.partida_gane, gane.titulo)
        assertNull(gane.motivo)

        val ganePorAbandono = textoFinalDe(marcadorDe(7, estado = "terminada", resultado = "gane", motivoFin = "abandono"))
        assertEquals(R.string.partida_final_abandono_gane, ganePorAbandono.motivo)

        val perdiPorAbandono = textoFinalDe(marcadorDe(7, estado = "terminada", resultado = "perdi", motivoFin = "abandono"))
        assertEquals(R.string.partida_perdi, perdiPorAbandono.titulo)
        assertEquals(R.string.partida_final_abandono_perdi, perdiPorAbandono.motivo)

        val empate = textoFinalDe(marcadorDe(7, estado = "terminada", resultado = "empate", motivoFin = "completada"))
        assertEquals(R.string.partida_empate, empate.titulo)
    }
}
