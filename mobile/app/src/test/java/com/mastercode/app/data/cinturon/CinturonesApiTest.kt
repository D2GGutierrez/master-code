package com.mastercode.app.data.cinturon

import com.mastercode.app.data.auth.ErrorHttp
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

// HU51-T2 y HU51-T3: HttpCinturonesApi real contra un servidor falso del JDK. El JSON copia el de user-api
// (CinturonController): con el negro, siguiente y puntos_faltantes llegan como null.
class CinturonesApiTest {

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

    private fun api() = HttpCinturonesApi("http://127.0.0.1:${servidor.address.port}/")

    private fun codigoDelError(bloque: suspend () -> Unit): Int? = runBlocking {
        try {
            bloque()
            null
        } catch (e: ErrorHttp) {
            e.codigo
        }
    }

    @Test
    fun pideLosCinturonesConUnGetYElAccessDelEstudiante() = runTest {
        cuerpoRespuesta = """{"cinturones": []}"""

        api().cinturones("access-falso")

        assertEquals("GET", metodo)
        assertEquals("/api/yo/cinturones", ruta)
        assertEquals("Bearer access-falso", authorization)
    }

    @Test
    fun leeElCinturonYLoQueFaltaParaElSiguiente() = runTest {
        cuerpoRespuesta = """
            {"cinturones": [
              {"lenguaje": "python", "nivel": 2, "nombre": "amarillo", "puntos": 130,
               "siguiente": {"nivel": 3, "nombre": "naranja", "puntos_requeridos": 250}, "puntos_faltantes": 120},
              {"lenguaje": "java", "nivel": 1, "nombre": "blanco", "puntos": 10,
               "siguiente": {"nivel": 2, "nombre": "amarillo", "puntos_requeridos": 100}, "puntos_faltantes": 90}
            ]}
        """.trimIndent()

        val cinturones = api().cinturones("access-falso")

        assertEquals(2, cinturones.size)
        assertEquals(
            CinturonDelLenguaje("python", 2, "amarillo", 130, SiguienteCinturon(3, "naranja", 250), 120),
            cinturones[0],
        )
        assertEquals("java", cinturones[1].lenguaje)
        assertEquals(90, cinturones[1].puntosFaltantes)
    }

    // CA-51.4: el negro no tiene siguiente ni puntos faltantes.
    @Test
    fun elNegroLlegaSinSiguienteNiPuntosFaltantes() = runTest {
        cuerpoRespuesta = """
            {"cinturones": [
              {"lenguaje": "python", "nivel": 7, "nombre": "negro", "puntos": 2500, "siguiente": null, "puntos_faltantes": null}
            ]}
        """.trimIndent()

        val negro = api().cinturones("access-falso").single()

        assertNull(negro.siguiente)
        assertNull(negro.puntosFaltantes)
        assertTrue(negro.esElMaximo)
    }

    // Sin ningún acierto no hay cinturón: la lista va vacía.
    @Test
    fun sinAciertosLaListaVaVacia() = runTest {
        cuerpoRespuesta = """{"cinturones": []}"""

        assertEquals(emptyList<CinturonDelLenguaje>(), api().cinturones("access-falso"))
    }

    @Test
    fun unaSesionVencidaLlegaComoErrorHttp401() {
        codigoRespuesta = 401
        cuerpoRespuesta = """{"detail": "x"}"""

        assertEquals(401, codigoDelError { api().cinturones("access-viejo") })
    }

    @Test
    fun unCuerpoQueNoEsElEsperadoLlegaComoErrorHttp() {
        cuerpoRespuesta = """{"otra_cosa": 1}"""

        assertEquals(200, codigoDelError { api().cinturones("access-falso") })
    }

    @Test
    fun conSiguienteNoEsElMaximo() = runTest {
        cuerpoRespuesta = """
            {"cinturones": [
              {"lenguaje": "python", "nivel": 6, "nombre": "marron", "puntos": 1400,
               "siguiente": {"nivel": 7, "nombre": "negro", "puntos_requeridos": 2000}, "puntos_faltantes": 600}
            ]}
        """.trimIndent()

        assertFalse(api().cinturones("access-falso").single().esElMaximo)
    }
}
