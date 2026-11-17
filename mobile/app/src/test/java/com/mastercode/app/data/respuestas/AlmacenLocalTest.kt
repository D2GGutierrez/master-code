package com.mastercode.app.data.respuestas

import com.mastercode.app.data.problemas.RespuestaDelEstudiante
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// El paso a JSON de lo que se guarda en SharedPreferences (HU48-T3, HU55-T1).
class AlmacenLocalTest {

    @Test
    fun lasRespuestasPendientesVuelvenIgualQueSeGuardaron() {
        val lista = listOf(
            RespuestaDelEstudiante(7, "línea 1\nlínea \"2\"", null, 30, "intento-1"),
            RespuestaDelEstudiante(12, null, 31, null, "intento-2"),
        )

        assertEquals(lista, respuestasDesdeJson(lista.aJson()))
    }

    @Test
    fun unGuardadoVacioDanadoOAusenteDaUnaListaVacia() {
        assertEquals(emptyList<RespuestaDelEstudiante>(), respuestasDesdeJson(null))
        assertEquals(emptyList<RespuestaDelEstudiante>(), respuestasDesdeJson(""))
        assertEquals(emptyList<RespuestaDelEstudiante>(), respuestasDesdeJson("{no es json"))
        assertEquals(emptyList<RespuestaDelEstudiante>(), respuestasDesdeJson("""[{"problema_id": 1}]"""))
    }

    @Test
    fun elEstadoDeLaSesionVuelveIgualQueSeGuardo() {
        val estado = EstadoDeSesion(sesionId = 41, lenguaje = "python", ejercicioActualId = 12, aciertos = 3, respondidos = 5)

        assertEquals(estado, estadoDeSesionDesdeJson(estado.aJson()))
    }

    @Test
    fun unaSesionSinEjercicioActualSeConserva() {
        val estado = EstadoDeSesion(41, "java", null, 0, 0)

        assertEquals(estado, estadoDeSesionDesdeJson(estado.aJson()))
    }

    @Test
    fun unEstadoDeSesionAusenteODanadoDaNull() {
        assertNull(estadoDeSesionDesdeJson(null))
        assertNull(estadoDeSesionDesdeJson("{}"))
        assertNull(estadoDeSesionDesdeJson("basura"))
    }
}
