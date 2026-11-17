package com.mastercode.app.data.perfil

import com.mastercode.app.data.auth.ErrorHttp
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

// HttpZonaHorariaApi real contra un servidor falso del JDK, como ProblemaApiTest. El token es inventado.
class ZonaHorariaApiTest {

    private lateinit var servidor: HttpServer
    private var metodo: String? = null
    private var ruta: String? = null
    private var authorization: String? = null
    private var contentType: String? = null
    private var cuerpoRecibido: String? = null
    private var codigoRespuesta = 204

    @Before
    fun levantarServidor() {
        servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servidor.createContext("/") { intercambio ->
            metodo = intercambio.requestMethod
            ruta = intercambio.requestURI.path
            authorization = intercambio.requestHeaders.getFirst("Authorization")
            contentType = intercambio.requestHeaders.getFirst("Content-Type")
            cuerpoRecibido = intercambio.requestBody.readBytes().toString(Charsets.UTF_8)
            // Un 204 va sin cuerpo (longitud -1); los errores llevan el detalle.
            if (codigoRespuesta == 204) {
                intercambio.sendResponseHeaders(204, -1)
            } else {
                val bytes = """{"detail": "zona_horaria debe ser un nombre de región."}""".toByteArray()
                intercambio.sendResponseHeaders(codigoRespuesta, bytes.size.toLong())
                intercambio.responseBody.use { it.write(bytes) }
            }
            intercambio.close()
        }
        servidor.start()
    }

    @After
    fun apagarServidor() {
        servidor.stop(0)
    }

    private fun api() = HttpZonaHorariaApi("http://127.0.0.1:${servidor.address.port}/")

    @Test
    fun mandaLaZonaPorPutConElBearerYElCuerpoEnJson() = runTest {
        api().guardar("access-falso", "America/Lima")

        assertEquals("PUT", metodo)
        assertEquals("/api/yo/zona-horaria", ruta)
        assertEquals("Bearer access-falso", authorization)
        assertEquals("application/json; charset=utf-8", contentType)
        assertEquals("America/Lima", JSONObject(cuerpoRecibido!!).getString("zona_horaria"))
    }

    @Test
    fun unaZonaConBarrasVaTalCualEnElCuerpo() = runTest {
        api().guardar("access-falso", "America/Argentina/Buenos_Aires")

        assertEquals("America/Argentina/Buenos_Aires", JSONObject(cuerpoRecibido!!).getString("zona_horaria"))
    }

    @Test
    fun un401LlegaComoErrorHttp() = runTest {
        codigoRespuesta = 401

        assertEquals(401, codigoDelError { api().guardar("vencido", "America/Lima") })
    }

    @Test
    fun un400PorUnaZonaQueNoValeLlegaComoErrorHttp() = runTest {
        codigoRespuesta = 400

        assertEquals(400, codigoDelError { api().guardar("access-falso", "GMT+05:00") })
    }

    @Test
    fun unCodigoDeExitoQueNoEsElEsperadoSeTrataComoError() = runTest {
        codigoRespuesta = 200

        assertEquals(200, codigoDelError { api().guardar("access-falso", "America/Lima") })
    }

    private suspend fun codigoDelError(llamada: suspend () -> Unit): Int? = try {
        llamada()
        null
    } catch (e: ErrorHttp) {
        e.codigo
    }
}
