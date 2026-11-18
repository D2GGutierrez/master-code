package com.mastercode.app.data.respuestas

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import com.mastercode.app.data.problemas.HttpProblemaApi
import com.mastercode.app.data.problemas.ProblemaApi
import com.mastercode.app.data.problemas.ProblemaDelEjercicio
import com.mastercode.app.data.problemas.ProblemaRepository
import com.mastercode.app.data.problemas.RespuestaDelEstudiante
import com.mastercode.app.data.problemas.ResultadoDeRespuesta
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.net.ServerSocket

// HU48-T4 (CA-48.4, CA-48.5): el modo avión, con el cliente HTTP de verdad (HttpProblemaApi) contra un servidor falso del
// JDK que se porta como user-api: registra cada respuesta por su intento_id y no la cuenta dos veces. «Sin señal» es una
// dirección donde nadie escucha (la conexión se rechaza al instante, como en el teléfono); «con señal», el servidor.
// La prueba en un teléfono real está en docs/recorrido-aceptacion-movil.md (paso del modo avión).
class ModoAvionRespuestasTest {

    private lateinit var servidor: HttpServer
    private lateinit var direccionSinSenal: String
    private var conSenal = false
    // Si es true, el servidor registra la respuesta pero corta antes de contestar: el teléfono la envió y perdió la señal
    // justo después, sin saber si llegó.
    private var cortarAntesDeContestar = false

    // Lo que ve el servidor: cuántas peticiones llegaron y cuántas respuestas distintas registró (por intento_id).
    private var peticiones = 0
    private val registradas = linkedSetOf<String>()

    private val almacen = MemoriaPendientes()
    private val auth = AuthRepository(FakeAuthApi(), MemoriaTokenStorage(refresh = "refresh-guardado"))

    private val respuesta = RespuestaDelEstudiante(7, "5", null, null, "intento-1")

    @Before
    fun prepararElServidorYLaDireccionMuerta() {
        servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servidor.createContext("/api/problemas/") { intercambio ->
            peticiones++
            val cuerpo = JSONObject(intercambio.requestBody.readBytes().toString(Charsets.UTF_8))
            registradas += cuerpo.getString("intento_id")
            if (cortarAntesDeContestar) {
                intercambio.close()
                return@createContext
            }
            val bytes = """{"resultado": "correcto", "intentos_restantes": 2}""".toByteArray()
            intercambio.sendResponseHeaders(200, bytes.size.toLong())
            intercambio.responseBody.use { it.write(bytes) }
            intercambio.close()
        }
        servidor.start()
        // Un puerto que se abre y se cierra: nadie escucha ahí.
        direccionSinSenal = "http://127.0.0.1:${ServerSocket(0).use { it.localPort }}/"
    }

    @After
    fun apagarElServidor() {
        servidor.stop(0)
    }

    // Una API que apunta al servidor o al vacío según haya señal en el momento de cada llamada.
    private val api = object : ProblemaApi {
        private fun real() = HttpProblemaApi(
            if (conSenal) "http://127.0.0.1:${servidor.address.port}/" else direccionSinSenal,
        )

        override suspend fun problema(access: String, id: Long): ProblemaDelEjercicio = real().problema(access, id)

        override suspend fun responder(access: String, respuesta: RespuestaDelEstudiante): ResultadoDeRespuesta =
            real().responder(access, respuesta)
    }

    private fun nuevoEnviador() = EnviadorDeRespuestas(ProblemaRepository(api, auth), almacen)

    // CA-48.4: en modo avión la respuesta no se pierde: queda guardada en el teléfono y nada llega al servidor.
    @Test
    fun enModoAvionLaRespuestaQuedaGuardadaYNoLlegaAlServidor() = runTest {
        val enviador = nuevoEnviador()

        val salida = enviador.enviar(respuesta)

        assertEquals(SalidaDeEnvio.Guardada, salida)
        assertEquals(listOf(respuesta), almacen.guardadas)
        assertEquals(0, peticiones)
    }

    // CA-48.5: al volver la señal se envía una sola vez, con el mismo intento_id, y ya no queda nada guardado.
    @Test
    fun alVolverLaSenalSeEnviaUnaSolaVezYSeLimpiaLoGuardado() = runTest {
        val enviador = nuevoEnviador()
        enviador.enviar(respuesta)

        conSenal = true
        enviador.reenviarPendientes()

        assertEquals(1, peticiones)
        assertEquals(setOf("intento-1"), registradas)
        assertEquals(emptyList<RespuestaDelEstudiante>(), almacen.guardadas)
    }

    // Otro aviso de señal (o varios seguidos) no vuelve a mandar lo que ya llegó.
    @Test
    fun avisosDeSenalRepetidosNoVuelvenAEnviarLaRespuesta() = runTest {
        val enviador = nuevoEnviador()
        enviador.enviar(respuesta)
        conSenal = true

        enviador.reenviarPendientes()
        enviador.reenviarPendientes()
        enviador.reenviarPendientes()

        assertEquals(1, peticiones)
        assertEquals(1, registradas.size)
    }

    // Mientras siga sin señal, tocar «Enviar» otra vez no duplica lo guardado ni llega nada al servidor.
    @Test
    fun reintentarSinSenalNoDuplicaLaRespuestaGuardada() = runTest {
        val enviador = nuevoEnviador()

        enviador.enviar(respuesta)
        enviador.enviar(respuesta)
        enviador.reenviarPendientes()

        assertEquals(listOf(respuesta), almacen.guardadas)
        assertEquals(0, peticiones)
    }

    // Se cierra la app con la respuesta guardada y se abre otra vez con señal: se envía una sola vez.
    @Test
    fun alReabrirLaAppConSenalSeEnviaLoQueQuedoGuardado() = runTest {
        nuevoEnviador().enviar(respuesta)

        conSenal = true
        val enviadorNuevo = nuevoEnviador()
        enviadorNuevo.reenviarPendientes()

        assertEquals(1, peticiones)
        assertEquals(setOf("intento-1"), registradas)
        assertEquals(emptyList<RespuestaDelEstudiante>(), almacen.guardadas)
    }

    // El peor caso: la respuesta llegó, pero la señal se fue antes de que el teléfono lo supiera. El teléfono la guarda y la
    // reenvía; el servidor la reconoce por su intento_id y la registra una sola vez (CA-48.5).
    @Test
    fun siLaRespuestaLlegoPeroSeCortoLaSenalElReenvioNoLaRegistraDosVeces() = runTest {
        val enviador = nuevoEnviador()
        conSenal = true
        cortarAntesDeContestar = true

        val salida = enviador.enviar(respuesta)

        assertEquals(SalidaDeEnvio.Guardada, salida)
        assertEquals(setOf("intento-1"), registradas)

        cortarAntesDeContestar = false
        enviador.reenviarPendientes()

        assertTrue(peticiones >= 2)
        // Por muchas peticiones que lleguen, la respuesta es una sola.
        assertEquals(setOf("intento-1"), registradas)
        assertEquals(emptyList<RespuestaDelEstudiante>(), almacen.guardadas)
    }

    // Dos ejercicios respondidos sin señal: al volver se envían los dos, en orden, cada uno una vez.
    @Test
    fun dosRespuestasSinSenalSeEnvianAlVolverCadaUnaUnaVez() = runTest {
        val enviador = nuevoEnviador()
        val otra = RespuestaDelEstudiante(8, "x", null, null, "intento-2")
        enviador.enviar(respuesta)
        enviador.enviar(otra)

        conSenal = true
        enviador.reenviarPendientes()
        enviador.reenviarPendientes()

        assertEquals(2, peticiones)
        assertEquals(listOf("intento-1", "intento-2"), registradas.toList())
        assertEquals(emptyList<RespuestaDelEstudiante>(), almacen.guardadas)
    }
}
