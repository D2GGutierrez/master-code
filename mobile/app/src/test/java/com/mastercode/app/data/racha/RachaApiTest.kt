package com.mastercode.app.data.racha

import com.mastercode.app.data.auth.ErrorHttp
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

// HU49-T3: HttpRachaApi real contra un servidor falso del JDK. El JSON copia el de user-api (RachaController):
// {racha_actual, practico_hoy, por_lenguaje[{lenguaje, racha_actual, racha_maxima, practico_hoy}]}.
class RachaApiTest {

    private lateinit var servidor: HttpServer
    private var metodo: String? = null
    private var ruta: String? = null
    private var authorization: String? = null
    private var codigoRespuesta = 200
    private var cuerpoRespuesta = ""

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
            intercambio.close()
        }
        servidor.start()
    }

    @After
    fun apagarServidor() {
        servidor.stop(0)
    }

    private fun api() = HttpRachaApi("http://127.0.0.1:${servidor.address.port}/")

    private fun codigoDelError(bloque: suspend () -> Unit): Int? = kotlinx.coroutines.runBlocking {
        try {
            bloque()
            null
        } catch (e: ErrorHttp) {
            e.codigo
        }
    }

    @Test
    fun pideLaRachaConUnGetYElAccessDelEstudiante() = runTest {
        cuerpoRespuesta = """{"racha_actual": 0, "practico_hoy": false, "por_lenguaje": []}"""

        api().racha("access-falso")

        assertEquals("GET", metodo)
        assertEquals("/api/yo/racha", ruta)
        assertEquals("Bearer access-falso", authorization)
    }

    @Test
    fun leeLaRachaGlobalYLaDeCadaLenguaje() = runTest {
        cuerpoRespuesta = """
            {"racha_actual": 5, "practico_hoy": true, "por_lenguaje": [
              {"lenguaje": "python", "racha_actual": 5, "racha_maxima": 9, "practico_hoy": true},
              {"lenguaje": "java", "racha_actual": 2, "racha_maxima": 2, "practico_hoy": false}
            ]}
        """.trimIndent()

        val racha = api().racha("access-falso")

        assertEquals(5, racha.rachaActual)
        assertTrue(racha.practicoHoy)
        assertEquals(
            listOf(
                RachaDeLenguaje("python", 5, 9, true),
                RachaDeLenguaje("java", 2, 2, false),
            ),
            racha.porLenguaje,
        )
    }

    // Un estudiante que nunca ha acertado: racha 0 y sin lenguajes.
    @Test
    fun sinActividadLaRachaEsCeroYNoHayLenguajes() = runTest {
        cuerpoRespuesta = """{"racha_actual": 0, "practico_hoy": false, "por_lenguaje": []}"""

        val racha = api().racha("access-falso")

        assertEquals(0, racha.rachaActual)
        assertFalse(racha.practicoHoy)
        assertEquals(emptyList<RachaDeLenguaje>(), racha.porLenguaje)
    }

    @Test
    fun unaSesionVencidaLlegaComoErrorHttp401() {
        codigoRespuesta = 401
        cuerpoRespuesta = """{"detail": "x"}"""

        assertEquals(401, codigoDelError { api().racha("access-viejo") })
    }

    @Test
    fun unErrorDelServidorLlegaComoErrorHttp500() {
        codigoRespuesta = 500
        cuerpoRespuesta = """{"detail": "x"}"""

        assertEquals(500, codigoDelError { api().racha("access-falso") })
    }

    // Un cuerpo que no es el esperado no revienta la app: llega como ErrorHttp.
    @Test
    fun unCuerpoQueNoEsElEsperadoLlegaComoErrorHttp() {
        cuerpoRespuesta = "esto no es json"

        assertEquals(200, codigoDelError { api().racha("access-falso") })
    }
}
