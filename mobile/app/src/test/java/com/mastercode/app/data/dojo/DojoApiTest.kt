package com.mastercode.app.data.dojo

import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.problemas.OpcionDelEjercicio
import com.mastercode.app.data.problemas.ProblemaDelEjercicio
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

// HttpDojoApi real contra un servidor falso del JDK. El JSON copia el de user-api (DojoController): la sesión iniciada
// y el ejercicio entregado, que es el mismo detalle de GET /api/problemas/{id}.
class DojoApiTest {

    private lateinit var servidor: HttpServer
    private var metodo: String? = null
    private var ruta: String? = null
    private var authorization: String? = null
    private var cuerpoRecibido: String? = null
    private var codigoRespuesta = 200
    private var cuerpoRespuesta = ""

    @Before
    fun levantarServidor() {
        servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servidor.createContext("/") { intercambio ->
            metodo = intercambio.requestMethod
            ruta = intercambio.requestURI.path
            authorization = intercambio.requestHeaders.getFirst("Authorization")
            cuerpoRecibido = intercambio.requestBody.readBytes().toString(Charsets.UTF_8)
            val bytes = cuerpoRespuesta.toByteArray()
            // Un 204 no lleva cuerpo.
            intercambio.sendResponseHeaders(codigoRespuesta, if (codigoRespuesta == 204) -1 else bytes.size.toLong())
            if (codigoRespuesta != 204) intercambio.responseBody.use { it.write(bytes) }
            intercambio.close()
        }
        servidor.start()
    }

    @After
    fun apagarServidor() {
        servidor.stop(0)
    }

    private fun api() = HttpDojoApi("http://127.0.0.1:${servidor.address.port}/")

    @Test
    fun iniciaLaSesionConElLenguajeYLeeSuIdYNivel() = runTest {
        codigoRespuesta = 201
        cuerpoRespuesta = """{"id": 41, "lenguaje": "python", "nivel": "facil"}"""

        val sesion = api().iniciar("access-falso", "python")

        assertEquals("POST", metodo)
        assertEquals("/api/dojo/sesiones", ruta)
        assertEquals("Bearer access-falso", authorization)
        assertEquals("python", JSONObject(cuerpoRecibido!!).getString("lenguaje"))
        assertEquals(SesionIniciada(41, "python", "facil"), sesion)
    }

    @Test
    fun pideElSiguienteEjercicioDeLaSesionYLeeLasAlternativas() = runTest {
        cuerpoRespuesta = """
            {"id": 12, "curso_id": 10, "titulo": "Elige", "enunciado": "¿Cuál imprime 5?", "tipo_ejercicio": "opcion_multiple",
             "opciones": [{"id": 31, "texto": "print(5)"}, {"id": 32, "texto": "print(4)"}], "intentos_restantes": 3}
        """.trimIndent()

        val problema = api().siguiente("access-falso", 41)

        assertEquals("POST", metodo)
        assertEquals("/api/dojo/sesiones/41/siguiente", ruta)
        assertEquals(
            ProblemaDelEjercicio(
                12, "Elige", "¿Cuál imprime 5?", "opcion_multiple",
                listOf(OpcionDelEjercicio(31, "print(5)"), OpcionDelEjercicio(32, "print(4)")), 3,
            ),
            problema,
        )
    }

    // 204: ya no quedan ejercicios.
    @Test
    fun unaRespuestaSinContenidoSignificaQueNoQuedanEjercicios() = runTest {
        codigoRespuesta = 204

        assertNull(api().siguiente("access-falso", 41))
    }

    @Test
    fun unaSesionTerminadaLlegaComoErrorHttp409() = runTest {
        codigoRespuesta = 409
        cuerpoRespuesta = """{"detail": "Esta sesión ya terminó."}"""

        val codigo = try {
            api().siguiente("access-falso", 41)
            null
        } catch (e: ErrorHttp) {
            e.codigo
        }

        assertEquals(409, codigo)
    }
}
