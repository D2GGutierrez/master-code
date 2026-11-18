package com.mastercode.app.data.dispositivo

import com.mastercode.app.data.auth.ErrorHttp
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

// HU52-T1: HttpDispositivoApi real contra un servidor falso del JDK. Copia lo que hace user-api (DispositivosController):
// PUT /api/dispositivo con {"token": ...} y 204 sin cuerpo.
class DispositivoApiTest {

    private lateinit var servidor: HttpServer
    private var metodo: String? = null
    private var ruta: String? = null
    private var authorization: String? = null
    private var cuerpoRecibido: String? = null
    private var codigoRespuesta = 204

    @Before
    fun levantarServidor() {
        servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servidor.createContext("/") { intercambio ->
            metodo = intercambio.requestMethod
            ruta = intercambio.requestURI.path
            authorization = intercambio.requestHeaders.getFirst("Authorization")
            cuerpoRecibido = intercambio.requestBody.readBytes().toString(Charsets.UTF_8)
            val bytes = """{"detail": "x"}""".toByteArray()
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

    private fun api() = HttpDispositivoApi("http://127.0.0.1:${servidor.address.port}/")

    @Test
    fun registraElTokenConUnPutYElAccessDelEstudiante() = runTest {
        api().registrar("access-falso", "token-fcm-123")

        assertEquals("PUT", metodo)
        assertEquals("/api/dispositivo", ruta)
        assertEquals("Bearer access-falso", authorization)
        assertEquals("token-fcm-123", JSONObject(cuerpoRecibido!!).getString("token"))
    }

    @Test
    fun unTokenRechazadoLlegaComoErrorHttp400() = runTest {
        codigoRespuesta = 400

        val codigo = try {
            api().registrar("access-falso", "")
            null
        } catch (e: ErrorHttp) {
            e.codigo
        }

        assertEquals(400, codigo)
    }

    @Test
    fun unaSesionVencidaLlegaComoErrorHttp401() = runTest {
        codigoRespuesta = 401

        val codigo = try {
            api().registrar("access-viejo", "token-fcm-123")
            null
        } catch (e: ErrorHttp) {
            e.codigo
        }

        assertEquals(401, codigo)
    }

    // Cualquier cosa que no sea 204 (por ejemplo un 200 con cuerpo) no se toma por éxito.
    @Test
    fun unaRespuestaQueNoEs204SeTomaPorError() = runTest {
        codigoRespuesta = 200

        val codigo = try {
            api().registrar("access-falso", "token-fcm-123")
            null
        } catch (e: ErrorHttp) {
            e.codigo
        }

        assertEquals(200, codigo)
    }
}
