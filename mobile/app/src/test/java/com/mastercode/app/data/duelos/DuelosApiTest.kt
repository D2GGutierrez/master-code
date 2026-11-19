package com.mastercode.app.data.duelos

import com.mastercode.app.data.auth.ErrorHttp
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress

// HU57-T3, H8, HU59-T3 y HU60-T3: HttpDuelosApi real contra un servidor falso del JDK que copia lo que hace user-api
// (PartidasController): JSON en snake_case, campos null sin omitir en el marcador y el historial, ETag en el marcador.
class DuelosApiTest {

    private class Llamada(val metodo: String, val ruta: String, val consulta: String?, val authorization: String?, val siNoCambio: String?, val cuerpo: String)

    private lateinit var servidor: HttpServer
    private val llamadas = mutableListOf<Llamada>()
    private var codigo = 200
    private var cuerpo = ""
    private var etag: String? = null
    private var demoraMs = 0L

    @Before
    fun levantarServidor() {
        servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servidor.createContext("/") { intercambio ->
            llamadas += Llamada(
                intercambio.requestMethod,
                intercambio.requestURI.path,
                intercambio.requestURI.query,
                intercambio.requestHeaders.getFirst("Authorization"),
                intercambio.requestHeaders.getFirst("If-None-Match"),
                intercambio.requestBody.readBytes().toString(Charsets.UTF_8),
            )
            if (demoraMs > 0) Thread.sleep(demoraMs)
            // Como Spring: si el ETag que manda la app es el actual, 304 sin cuerpo.
            val respuesta = if (etag != null && etag == intercambio.requestHeaders.getFirst("If-None-Match")) 304 else codigo
            val bytes = cuerpo.toByteArray()
            if (etag != null) intercambio.responseHeaders.add("ETag", etag)
            val sinCuerpo = respuesta == 204 || respuesta == 304
            intercambio.sendResponseHeaders(respuesta, if (sinCuerpo) -1 else bytes.size.toLong())
            if (!sinCuerpo) intercambio.responseBody.use { it.write(bytes) }
            intercambio.close()
        }
        servidor.start()
    }

    @After
    fun apagarServidor() {
        servidor.stop(0)
    }

    private fun api() = HttpDuelosApi("http://127.0.0.1:${servidor.address.port}/")

    private fun marcadorJson(estado: String = "en_curso", resultado: String? = null, motivo: String? = null, yo: Int = 1, rival: Int = 0) = """
        {"partida_id":7,"curso_id":3,"estado":"$estado","total_ejercicios":5,
         "yo":{"alias":"ana","puntaje":$yo,"respondidos":2},"rival":{"alias":"beto","puntaje":$rival,"respondidos":1},
         "rival_conectado":true,"resultado":${resultado?.let { "\"$it\"" }},"motivo_fin":${motivo?.let { "\"$it\"" }}}
    """.trimIndent()

    private suspend fun codigoDe(bloque: suspend () -> Unit): Int? = try {
        bloque()
        null
    } catch (e: ErrorHttp) {
        e.codigo
    }

    @Test
    fun losCompanerosVienenConSuAliasYElAccessDelEstudiante() = runTest {
        cuerpo = """[{"id":4,"alias":"beto"},{"id":9,"alias":"carla"}]"""

        val lista = api().companeros("access-falso", 3)

        assertEquals(listOf(Companero(4, "beto"), Companero(9, "carla")), lista)
        assertEquals("/api/cursos/3/companeros", llamadas.single().ruta)
        assertEquals("Bearer access-falso", llamadas.single().authorization)
    }

    @Test
    fun retarEnviaElCursoYElRivalYLeeElRetoCreado() = runTest {
        codigo = 201
        cuerpo = """{"id":11,"curso_id":3,"estado":"pendiente","rival":"beto","total_ejercicios":5}"""

        val reto = api().retar("a", 3, 4)

        assertEquals(RetoCreado(11, 3, "beto", 5), reto)
        val llamada = llamadas.single()
        assertEquals("POST", llamada.metodo)
        assertEquals("/api/partidas", llamada.ruta)
        assertEquals(3L, JSONObject(llamada.cuerpo).getLong("curso_id"))
        assertEquals(4L, JSONObject(llamada.cuerpo).getLong("rival_id"))
    }

    @Test
    fun unRetoRepetidoLlegaComoErrorHttp409() = runTest {
        codigo = 409
        cuerpo = """{"detail":"Ya hay un reto."}"""

        assertEquals(409, codigoDe { api().retar("a", 3, 4) })
    }

    @Test
    fun lasInvitacionesTraenAlRetadorYCuandoVencen() = runTest {
        cuerpo = """[{"id":11,"curso_id":3,"retador":"ana","fecha_creacion":"2026-11-22T10:00:00Z","vence_en":"2026-11-22T10:05:00Z"}]"""

        val lista = api().invitaciones("a")

        assertEquals(listOf(Invitacion(11, 3, "ana", "2026-11-22T10:05:00Z")), lista)
    }

    @Test
    fun aceptarDevuelveElMarcadorInicial() = runTest {
        cuerpo = marcadorJson(yo = 0)

        val marcador = api().aceptar("a", 7)

        assertEquals("POST", llamadas.single().metodo)
        assertEquals("/api/partidas/7/aceptar", llamadas.single().ruta)
        assertEquals("en_curso", marcador.estado)
        assertEquals(JugadorDelMarcador("beto", 0, 1), marcador.rival)
        assertNull(marcador.resultado)
        assertNull(marcador.motivoFin)
    }

    @Test
    fun rechazarContestaConUn204SinCuerpo() = runTest {
        codigo = 204

        api().rechazar("a", 7)

        assertEquals("/api/partidas/7/rechazar", llamadas.single().ruta)
    }

    @Test
    fun elEjercicioDelTurnoTraeElTurnoYElProblema() = runTest {
        cuerpo = """{"turno":2,"total_ejercicios":5,"ejercicio":{"id":21,"curso_id":3,"titulo":"Suma","enunciado":"¿Qué imprime?",
            "tipo_ejercicio":"opcion_multiple","opciones":[{"id":1,"texto":"3"},{"id":2,"texto":"4"}]}}"""

        val ejercicio = api().ejercicio("a", 7)!!

        assertEquals(2, ejercicio.turno)
        assertEquals(5, ejercicio.totalEjercicios)
        assertEquals(21L, ejercicio.problema.id)
        assertEquals(listOf(1L, 2L), ejercicio.problema.opciones.map { it.id })
    }

    // 204: ya respondió todos y espera al rival.
    @Test
    fun sinEjercicioPendienteDevuelveNull() = runTest {
        codigo = 204

        assertNull(api().ejercicio("a", 7))
    }

    @Test
    fun responderEnviaElTurnoYLaRespuestaEscrita() = runTest {
        cuerpo = """{"resultado":"correcto","marcador":${marcadorJson()}}"""

        val respuesta = api().responder("a", 7, 2, "4", null)

        assertTrue(respuesta.acerto)
        assertNull(respuesta.respuestaCorrecta)
        assertEquals(1, respuesta.marcador.yo.puntaje)
        val enviado = JSONObject(llamadas.single().cuerpo)
        assertEquals(2, enviado.getInt("turno"))
        assertEquals("4", enviado.getString("respuesta"))
        assertFalse(enviado.has("opcion_id"))
    }

    @Test
    fun responderConOpcionEnviaLaAlternativaYLeeLaCorrectaSiFallo() = runTest {
        cuerpo = """{"resultado":"incorrecto","respuesta_correcta":"4","marcador":${marcadorJson()}}"""

        val respuesta = api().responder("a", 7, 3, null, 2L)

        assertFalse(respuesta.acerto)
        assertEquals("4", respuesta.respuestaCorrecta)
        val enviado = JSONObject(llamadas.single().cuerpo)
        assertEquals(2L, enviado.getLong("opcion_id"))
        assertFalse(enviado.has("respuesta"))
    }

    @Test
    fun unTurnoQueNoEsElSiguienteLlegaComoErrorHttp409() = runTest {
        codigo = 409

        assertEquals(409, codigoDe { api().responder("a", 7, 5, "x", null) })
    }

    @Test
    fun elMarcadorTerminadoTraeElResultadoYElMotivo() = runTest {
        cuerpo = marcadorJson(estado = "terminada", resultado = "gane", motivo = "abandono", yo = 3, rival = 1)
        etag = "\"abc\""

        val lectura = api().marcador("a", 7, null)

        val marcador = lectura.marcador!!
        assertEquals("gane", marcador.resultado)
        assertEquals("abandono", marcador.motivoFin)
        assertTrue(marcador.terminada)
        assertEquals("\"abc\"", lectura.etag)
        assertNull(llamadas.single().siNoCambio)
    }

    // HU59-T2 / HU59-T5: con el mismo ETag user-api contesta 304 y la app no vuelve a bajar nada.
    @Test
    fun siElMarcadorNoCambioElServidorContesta304YLaAppConservaElEtag() = runTest {
        cuerpo = marcadorJson()
        etag = "\"abc\""
        val primera = api().marcador("a", 7, null)

        val segunda = api().marcador("a", 7, primera.etag)

        assertNull(segunda.marcador)
        assertEquals("\"abc\"", segunda.etag)
        assertEquals("\"abc\"", llamadas.last().siNoCambio)
    }

    @Test
    fun siElMarcadorCambioConUnEtagViejoSeLeeElNuevo() = runTest {
        cuerpo = marcadorJson(yo = 2)
        etag = "\"nuevo\""

        val lectura = api().marcador("a", 7, "\"viejo\"")

        assertEquals(2, lectura.marcador!!.yo.puntaje)
        assertEquals("\"nuevo\"", lectura.etag)
    }

    // HU59-T5: una respuesta lenta sigue llegando completa mientras no pase el tiempo de espera de la conexión.
    @Test
    fun unaRespuestaLentaDelMarcadorSigueLlegando() = runTest {
        cuerpo = marcadorJson(yo = 4)
        demoraMs = 1_500

        val lectura = api().marcador("a", 7, null)

        assertEquals(4, lectura.marcador!!.yo.puntaje)
    }

    @Test
    fun sinServidorElMarcadorLanzaIOException() = runTest {
        val api = api()
        servidor.stop(0)

        val fallo = try {
            api.marcador("a", 7, null)
            null
        } catch (e: IOException) {
            e
        }

        assertTrue(fallo != null)
    }

    @Test
    fun elHistorialLeeLasPartidasYSusResultados() = runTest {
        cuerpo = """{"items":[
            {"id":7,"curso_id":3,"estado":"terminada","rival":"beto","mi_puntaje":4,"puntaje_rival":2,"resultado":"gane","motivo_fin":"completada","fecha_inicio":"2026-11-22T10:00:00Z","fecha_fin":"2026-11-22T10:08:00Z"},
            {"id":8,"curso_id":3,"estado":"en_curso","rival":"carla","mi_puntaje":1,"puntaje_rival":1,"resultado":null,"motivo_fin":null,"fecha_inicio":"2026-11-22T11:00:00Z","fecha_fin":null}],
            "pagina":1,"tamano":20,"total":2}"""

        val pagina = api().historial("a", 1, 20)

        assertEquals("pagina=1&tamano=20", llamadas.single().consulta)
        assertEquals(2, pagina.total)
        assertEquals("gane", pagina.items[0].resultado)
        assertEquals("2026-11-22T10:08:00Z", pagina.items[0].fecha)
        assertNull(pagina.items[1].resultado)
        assertEquals("2026-11-22T11:00:00Z", pagina.items[1].fecha)
    }

    @Test
    fun elRankingTraeLasFilasYMiPuesto() = runTest {
        cuerpo = """{"curso_id":3,"total":2,"pagina":1,"tamano":20,"items":[
            {"posicion":1,"alias":"ana","jugadas":4,"ganadas":3,"empatadas":1,"perdidas":0,"puntos":10,"porcentaje_victorias":75.0,"soy_yo":false},
            {"posicion":2,"alias":"beto","jugadas":4,"ganadas":1,"empatadas":1,"perdidas":2,"puntos":6,"porcentaje_victorias":25.0,"soy_yo":true}],
            "yo":{"posicion":2,"alias":"beto","jugadas":4,"ganadas":1,"empatadas":1,"perdidas":2,"puntos":6,"porcentaje_victorias":25.0,"soy_yo":true}}"""

        val ranking = api().ranking("a", 3, 1, 20)

        assertEquals("/api/cursos/3/ranking", llamadas.single().ruta)
        assertEquals(listOf(1, 2), ranking.items.map { it.posicion })
        assertEquals(75.0, ranking.items[0].porcentajeVictorias, 0.001)
        assertTrue(ranking.yo!!.soyYo)
    }

    // HU60-T4: quien todavía no jugó en el curso no tiene puesto.
    @Test
    fun sinPartidasElRankingNoTraeMiPuesto() = runTest {
        cuerpo = """{"curso_id":3,"total":0,"pagina":1,"tamano":20,"items":[],"yo":null}"""

        val ranking = api().ranking("a", 3, 1, 20)

        assertEquals(0, ranking.total)
        assertNull(ranking.yo)
    }

    @Test
    fun elRankingDeUnCursoAjenoLlegaComoErrorHttp403() = runTest {
        codigo = 403

        assertEquals(403, codigoDe { api().ranking("a", 99, 1, 20) })
    }

    @Test
    fun unCuerpoQueNoEsElEsperadoSeTrataComoErrorDelServidor() = runTest {
        cuerpo = """{"algo":"raro"}"""

        assertEquals(200, codigoDe { api().aceptar("a", 7) })
    }
}
