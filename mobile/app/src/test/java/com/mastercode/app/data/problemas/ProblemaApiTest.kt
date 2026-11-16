package com.mastercode.app.data.problemas

import com.mastercode.app.data.auth.ErrorHttp
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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

    private suspend fun codigoDelError(llamada: suspend () -> Unit): Int? = try {
        llamada()
        null
    } catch (e: ErrorHttp) {
        e.codigo
    }
}
