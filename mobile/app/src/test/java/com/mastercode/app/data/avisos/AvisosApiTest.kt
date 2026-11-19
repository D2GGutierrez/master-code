package com.mastercode.app.data.avisos

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress

// HU52-T4: la app manda los ajustes de avisos a user-api (PUT /api/yo/avisos con {"activados", "hora": "HH:mm"} y 204).
class AvisosApiTest {

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

    private fun api() = HttpAvisosApi("http://127.0.0.1:${servidor.address.port}/")

    @Test
    fun mandaLosAjustesConUnPutYLaHoraEnFormatoHHmm() = runTest {
        api().guardar("access-falso", false, "07:30")

        assertEquals("PUT", metodo)
        assertEquals("/api/yo/avisos", ruta)
        assertEquals("Bearer access-falso", authorization)
        val cuerpo = JSONObject(cuerpoRecibido!!)
        assertEquals(false, cuerpo.getBoolean("activados"))
        assertEquals("07:30", cuerpo.getString("hora"))
    }

    @Test
    fun unaHoraRechazadaLlegaComoErrorHttp400() = runTest {
        codigoRespuesta = 400

        val codigo = try {
            api().guardar("access-falso", true, "99:99")
            null
        } catch (e: ErrorHttp) {
            e.codigo
        }

        assertEquals(400, codigo)
    }

    @Test
    fun laHoraDeLosAjustesSeEscribeConCeros() {
        assertEquals("20:00", AjustesDeAvisos().horaComoTexto())
        assertEquals("07:05", AjustesDeAvisos(hora = 7, minuto = 5).horaComoTexto())
    }

    @Test
    fun elRepositorioEnviaLosAjustesDelTelefono() = runTest {
        val repositorio = AvisosRepository(api(), AuthRepository(FakeAuthApi(), MemoriaTokenStorage(refresh = "r")))

        val resultado = repositorio.enviar(AjustesDeAvisos(activados = true, hora = 18, minuto = 45))

        assertEquals(ResultadoEnvioDeAvisos.Enviado, resultado)
        assertEquals("18:45", JSONObject(cuerpoRecibido!!).getString("hora"))
    }

    @Test
    fun sinSenalElRepositorioDiceSinConexionYNoLanza() = runTest {
        val repositorio = AvisosRepository(object : AvisosApi {
            override suspend fun guardar(access: String, activados: Boolean, hora: String) = throw IOException("sin red")
        }, AuthRepository(FakeAuthApi(), MemoriaTokenStorage(refresh = "r")))

        assertEquals(ResultadoEnvioDeAvisos.SinConexion, repositorio.enviar(AjustesDeAvisos()))
    }
}
