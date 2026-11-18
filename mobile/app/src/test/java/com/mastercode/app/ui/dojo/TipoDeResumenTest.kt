package com.mastercode.app.ui.dojo

import com.mastercode.app.data.dojo.ResumenDeSesion
import com.mastercode.app.data.dojo.TemaFallado
import org.junit.Assert.assertEquals
import org.junit.Test

// HU56-T2 (CA-56.4 y CA-56.5): qué mensaje lleva el resumen según cómo le fue al estudiante.
class TipoDeResumenTest {

    private fun resumen(respondidos: Int, aciertos: Int, temas: List<TemaFallado> = emptyList()) =
        ResumenDeSesion("facil", respondidos, aciertos, aciertos * 10, temas, null, null)

    @Test
    fun sinResponderNadaNoHayNadaQueEvaluar() {
        assertEquals(TipoDeResumen.SinRespuestas, tipoDeResumen(resumen(0, 0)))
    }

    // CA-56.4: todo acertado, sin temas a repasar.
    @Test
    fun conTodoAcertadoEsPerfecto() {
        assertEquals(TipoDeResumen.Perfecto, tipoDeResumen(resumen(5, 5)))
        assertEquals(TipoDeResumen.Perfecto, tipoDeResumen(resumen(1, 1)))
    }

    // CA-56.5: ni un acierto, y hay temas para repasar.
    @Test
    fun sinNingunAciertoSeAnimaARepasar() {
        val temas = listOf(TemaFallado("bucles", 3), TemaFallado("listas", 2))

        assertEquals(TipoDeResumen.SinAciertos, tipoDeResumen(resumen(5, 0, temas)))
    }

    @Test
    fun conAciertosYFallosEsNormal() {
        assertEquals(TipoDeResumen.Normal, tipoDeResumen(resumen(5, 3, listOf(TemaFallado("bucles", 2)))))
        assertEquals(TipoDeResumen.Normal, tipoDeResumen(resumen(2, 1)))
    }

    // Un servidor que contara más aciertos que respuestas no rompe el mensaje: sigue siendo perfecto.
    @Test
    fun masAciertosQueRespuestasSigueSiendoPerfecto() {
        assertEquals(TipoDeResumen.Perfecto, tipoDeResumen(resumen(3, 4)))
    }
}
