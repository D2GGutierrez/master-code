package com.mastercode.app.data.problemas

import com.mastercode.app.data.auth.ErrorHttp
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

// HttpProblemaApi real contra un servidor falso del JDK, como MisCursosApiTest. El JSON copia el formato de
// GET /api/problemas/{id} (ProblemaDetalle de user-api): trae más campos de los que la app usa. El token es inventado.
class ProblemaApiTest {

    private lateinit var servidor: HttpServer
    private var metodo: String? = null
    private var ruta: String? = null
    private var authorization: String? = null
    private var contentType: String? = null
    private var cuerpoRecibido: String? = null
    private var codigoRespuesta = 200
    private var cuerpoRespuesta = """
        {"id": 7, "curso_id": 10, "titulo": "Suma en un print", "enunciado": "¿Qué imprime?\n\nprint(2 + 3)",
         "lenguaje": "python", "dificultad": "facil", "plataforma": "movil", "tipo_ejercicio": "prediccion_salida",
         "codigo_base": "", "casos_prueba": [], "opciones": []}
    """.trimIndent()

    @Before
    fun levantarServidor() {
        servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servidor.createContext("/") { intercambio ->
            metodo = intercambio.requestMethod
            ruta = intercambio.requestURI.path
            authorization = intercambio.requestHeaders.getFirst("Authorization")
            contentType = intercambio.requestHeaders.getFirst("Content-Type")
            cuerpoRecibido = intercambio.requestBody.readBytes().toString(Charsets.UTF_8)
            val bytes = cuerpoRespuesta.toByteArray()
            intercambio.sendResponseHeaders(codigoRespuesta, bytes.size.toLong())
            intercambio.responseBody.use { it.write(bytes) }
        }
        servidor.start()
    }

    @After
    fun apagarServidor() {
        servidor.stop(0)
    }

    private fun api() = HttpProblemaApi("http://127.0.0.1:${servidor.address.port}/")

    @Test
    fun pideElProblemaPorIdConElBearerYLeeLoQueLaPantallaMuestra() = runTest {
        val problema = api().problema("access-falso", 7)

        assertEquals("GET", metodo)
        assertEquals("/api/problemas/7", ruta)
        assertEquals("Bearer access-falso", authorization)
        assertEquals(
            ProblemaDelEjercicio(7, "Suma en un print", "¿Qué imprime?\n\nprint(2 + 3)", "prediccion_salida"),
            problema,
        )
    }

    @Test
    fun un404DeUserApiLlegaComoErrorHttp() = runTest {
        codigoRespuesta = 404
        cuerpoRespuesta = """{"detail": "No encontrado."}"""

        assertEquals(404, codigoDelError { api().problema("access-falso", 7) })
    }

    @Test
    fun unaRespuestaSinElFormatoEsperadoSeTrataComoErrorDelServidor() = runTest {
        cuerpoRespuesta = """{"id": 7}"""

        assertEquals(200, codigoDelError { api().problema("access-falso", 7) })
    }

    @Test
    fun leeLasAlternativasYLosIntentosQueQuedan() = runTest {
        cuerpoRespuesta = """
            {"id": 12, "curso_id": 10, "titulo": "Elige", "enunciado": "¿Cuál imprime 5?", "tipo_ejercicio": "opcion_multiple",
             "opciones": [{"id": 31, "texto": "print(5)"}, {"id": 32, "texto": "print(4)"}], "intentos_restantes": 2}
        """.trimIndent()

        val problema = api().problema("access-falso", 12)

        assertEquals(listOf(OpcionDelEjercicio(31, "print(5)"), OpcionDelEjercicio(32, "print(4)")), problema.opciones)
        assertEquals(2, problema.intentosRestantes)
    }

    @Test
    fun sinOpcionesNiIntentosLosDejaVaciosYNulos() = runTest {
        val problema = api().problema("access-falso", 7)

        assertEquals(emptyList<OpcionDelEjercicio>(), problema.opciones)
        assertNull(problema.intentosRestantes)
    }

    // HU45-T3: el cuerpo lleva los nombres en snake_case que espera user-api y solo lo que se contestó.
    @Test
    fun respondeConTextoYUnIdentificadorDeIntento() = runTest {
        cuerpoRespuesta = """{"resultado": "incorrecto", "respuesta_correcta": "5", "intentos_restantes": 2}"""

        val resultado = api().responder("access-falso", RespuestaDelEstudiante(7, "4", null, 30, "intento-abc"))

        assertEquals("POST", metodo)
        assertEquals("/api/problemas/7/respuesta", ruta)
        assertEquals("Bearer access-falso", authorization)
        assertEquals("application/json; charset=utf-8", contentType)
        val cuerpo = JSONObject(cuerpoRecibido!!)
        assertEquals("4", cuerpo.getString("respuesta"))
        assertEquals(30L, cuerpo.getLong("sesion_id"))
        assertEquals("intento-abc", cuerpo.getString("intento_id"))
        assertEquals(false, cuerpo.has("opcion_id"))
        assertEquals(ResultadoDeRespuesta(acerto = false, respuestaCorrecta = "5", intentosRestantes = 2), resultado)
    }

    @Test
    fun respondeConLaAlternativaElegidaYSinTextoNiSesion() = runTest {
        cuerpoRespuesta = """{"resultado": "correcto", "intentos_restantes": 1}"""

        val resultado = api().responder("access-falso", RespuestaDelEstudiante(12, null, 31, null, "intento-xyz"))

        val cuerpo = JSONObject(cuerpoRecibido!!)
        assertEquals(31L, cuerpo.getLong("opcion_id"))
        assertEquals(false, cuerpo.has("respuesta"))
        assertEquals(false, cuerpo.has("sesion_id"))
        // Al acertar user-api no manda respuesta_correcta.
        assertEquals(ResultadoDeRespuesta(acerto = true, respuestaCorrecta = null, intentosRestantes = 1), resultado)
    }

    @Test
    fun loQueUserApiRechazaAlResponderLlegaComoErrorHttp() = runTest {
        for (codigo in listOf(400, 404, 409)) {
            codigoRespuesta = codigo
            cuerpoRespuesta = """{"detail": "no"}"""

            val recibido = codigoDelError { api().responder("access-falso", RespuestaDelEstudiante(7, "4", null, null, "i")) }
            assertEquals(codigo, recibido)
        }
    }

    @Test
    fun unaRespuestaCorregidaSinElFormatoEsperadoSeTrataComoErrorDelServidor() = runTest {
        cuerpoRespuesta = """{"resultado": "correcto"}"""

        assertEquals(200, codigoDelError { api().responder("access-falso", RespuestaDelEstudiante(7, "5", null, null, "i")) })
    }

    private suspend fun codigoDelError(llamada: suspend () -> Unit): Int? = try {
        llamada()
        null
    } catch (e: ErrorHttp) {
        e.codigo
    }
}
