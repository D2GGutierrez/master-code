package com.mastercode.app.data.auth

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

// HttpAuthApi real contra un servidor falso del JDK: prueba el JSON y las cabeceras que salen
// de verdad por la red, sin admin-api ni emulador. Los tokens son textos inventados.
class LoginMovilEndToEndTest {

    // Lo que llegó al servidor falso en la última petición.
    private class Recibida(val metodo: String, val ruta: String, val authorization: String?, val cuerpo: String)

    private lateinit var servidor: HttpServer
    private var recibida: Recibida? = null
    private var codigoRespuesta = 200
    private var cuerpoRespuesta = """{"access": "access-falso", "refresh": "refresh-falso"}"""

    @Before
    fun levantarServidor() {
        // Puerto 0: el sistema elige uno libre, así las pruebas no chocan con admin-api local.
        servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servidor.createContext("/") { intercambio ->
            recibida = Recibida(
                metodo = intercambio.requestMethod,
                ruta = intercambio.requestURI.path,
                authorization = intercambio.requestHeaders.getFirst("Authorization"),
                cuerpo = intercambio.requestBody.bufferedReader().use { it.readText() },
            )
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

    private fun api() = HttpAuthApi("http://127.0.0.1:${servidor.address.port}/")

    @Test
    fun loginEnviaClienteMobileConElCorreoYLaContrasena() = runTest {
        val tokens = api().login("ana@ejemplo.edu.pe", "clave-inventada")

        val peticion = recibida!!
        assertEquals("POST", peticion.metodo)
        assertEquals("/api/auth/login/", peticion.ruta)
        val json = JSONObject(peticion.cuerpo)
        assertEquals("mobile", json.getString("cliente"))
        assertEquals("ana@ejemplo.edu.pe", json.getString("email"))
        assertEquals("clave-inventada", json.getString("password"))
        assertEquals(Tokens(access = "access-falso", refresh = "refresh-falso"), tokens)
    }

    @Test
    fun loginNoMandaEncabezadoAuthorization() = runTest {
        api().login("ana@ejemplo.edu.pe", "clave-inventada")

        // Quien inicia sesión todavía no tiene token: no debe enviar ninguno.
        assertNull(recibida!!.authorization)
    }

    @Test
    fun renovacionRechazadaCon401SeTrataComoSesionExpirada() = runTest {
        codigoRespuesta = 401
        cuerpoRespuesta = """{"detail": "La sesión no es válida o ya venció."}"""
        val repository = AuthRepository(api(), MemoriaTokenStorage(refresh = "refresh-vencido"))

        val resultado = repository.renovar()

        assertEquals(ResultadoRenovacion.SesionExpirada, resultado)
        // La petición real fue a la renovación con el refresh guardado.
        val peticion = recibida!!
        assertEquals("/api/auth/refresh/", peticion.ruta)
        assertEquals("refresh-vencido", JSONObject(peticion.cuerpo).getString("refresh"))
    }
}
