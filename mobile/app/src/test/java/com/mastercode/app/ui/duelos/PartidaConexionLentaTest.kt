package com.mastercode.app.ui.duelos

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import com.mastercode.app.data.duelos.DuelosRepository
import com.mastercode.app.data.duelos.HttpDuelosApi
import com.mastercode.app.ui.ejercicios.EstadoDeEnvio
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.Executors

// HU59-T5: la partida con conexión lenta o que se cae. Usa toda la pila real (HttpDuelosApi, DuelosRepository y
// PartidaViewModel) contra un servidor falso del JDK que se puede ralentizar o cortar, como una red mala.
@OptIn(ExperimentalCoroutinesApi::class)
class PartidaConexionLentaTest {

    private lateinit var servidor: HttpServer
    @Volatile private var demoraDelMarcadorMs = 0L
    @Volatile private var caido = false
    @Volatile private var puntajeDelRival = 0
    private val consultasConEtag = mutableListOf<String?>()

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        // Varios hilos: una respuesta lenta del marcador no debe frenar las demás, como en user-api.
        servidor.executor = Executors.newCachedThreadPool()
        servidor.createContext("/api/partidas/7/marcador") { intercambio ->
            if (caido) {
                intercambio.close()
                return@createContext
            }
            val etag = "\"rival-$puntajeDelRival\""
            val siNoCambio = intercambio.requestHeaders.getFirst("If-None-Match")
            synchronized(consultasConEtag) { consultasConEtag += siNoCambio }
            if (demoraDelMarcadorMs > 0) Thread.sleep(demoraDelMarcadorMs)
            intercambio.responseHeaders.add("ETag", etag)
            if (etag == siNoCambio) {
                intercambio.sendResponseHeaders(304, -1)
            } else {
                responder(intercambio, 200, marcadorJson())
            }
            intercambio.close()
        }
        servidor.createContext("/api/partidas/7/ejercicio") { intercambio ->
            responder(
                intercambio, 200,
                """{"turno":1,"total_ejercicios":3,"ejercicio":{"id":21,"titulo":"Suma","enunciado":"¿Qué imprime?","tipo_ejercicio":"prediccion_salida"}}""",
            )
            intercambio.close()
        }
        servidor.createContext("/api/partidas/7/respuesta") { intercambio ->
            responder(intercambio, 200, """{"resultado":"correcto","marcador":${marcadorJson(yo = 1)}}""")
            intercambio.close()
        }
        servidor.start()
    }

    @After
    fun apagar() {
        servidor.stop(0)
        (servidor.executor as java.util.concurrent.ExecutorService).shutdownNow()
        Dispatchers.resetMain()
    }

    private fun marcadorJson(yo: Int = 0) = """
        {"partida_id":7,"curso_id":3,"estado":"en_curso","total_ejercicios":3,
         "yo":{"alias":"ana","puntaje":$yo,"respondidos":$yo},"rival":{"alias":"beto","puntaje":$puntajeDelRival,"respondidos":$puntajeDelRival},
         "rival_conectado":true,"resultado":null,"motivo_fin":null}
    """.trimIndent()

    private fun responder(intercambio: HttpExchange, codigo: Int, cuerpo: String) {
        val bytes = cuerpo.toByteArray()
        intercambio.sendResponseHeaders(codigo, bytes.size.toLong())
        intercambio.responseBody.use { it.write(bytes) }
    }

    private fun crear(): PartidaViewModel {
        val auth = AuthRepository(FakeAuthApi(), MemoriaTokenStorage(refresh = "refresh-guardado"))
        val api = HttpDuelosApi("http://127.0.0.1:${servidor.address.port}/")
        return PartidaViewModel(DuelosRepository(api, auth), 7)
    }

    // Espera (tiempo real) a que pase algo que ocurre en otro hilo.
    private suspend fun esperar(condicion: () -> Boolean) = withContext(Dispatchers.IO) {
        repeat(200) {
            if (condicion()) return@withContext
            Thread.sleep(25)
        }
        throw AssertionError("No pasó lo esperado a tiempo")
    }

    // Mientras una consulta lenta del marcador sigue en camino, el estudiante puede responder y ver su corrección: lo lento
    // no bloquea lo demás.
    @Test
    fun unMarcadorLentoNoBloqueaLaRespuestaDelEstudiante() = runTest {
        val viewModel = crear()
        viewModel.sondear()
        viewModel.cargarEjercicio()
        val ejercicio = (viewModel.uiState.value.fase as FaseDePartida.Jugando).ejercicio

        demoraDelMarcadorMs = 1_500
        puntajeDelRival = 2
        val consultaLenta = async { viewModel.sondear() }
        ejercicio.alCambiarRespuesta("4")
        ejercicio.enviar()
        esperar { ejercicio.uiState.value.envio is EstadoDeEnvio.Resultado }

        assertFalse("La consulta lenta ya había terminado", consultaLenta.isCompleted)
        // El marcador que vino con la corrección ya muestra el punto propio, sin esperar a la consulta lenta.
        assertEquals(1, viewModel.uiState.value.marcador!!.yo.puntaje)

        consultaLenta.await()
        assertEquals(2, viewModel.uiState.value.marcador!!.rival.puntaje)
        assertFalse(viewModel.uiState.value.sinSenal)
    }

    // Si la conexión se corta, la partida sigue en pantalla con el aviso; al volver la señal, el marcador se pone al día.
    @Test
    fun siSeCaeLaConexionLaPartidaSigueYSeRecuperaAlVolver() = runTest {
        val viewModel = crear()
        viewModel.sondear()
        viewModel.cargarEjercicio()
        val antes = viewModel.uiState.value

        caido = true
        viewModel.sondear()
        assertTrue(viewModel.uiState.value.sinSenal)
        assertEquals(antes.marcador, viewModel.uiState.value.marcador)
        assertTrue(viewModel.uiState.value.fase is FaseDePartida.Jugando)

        caido = false
        puntajeDelRival = 1
        viewModel.sondear()

        assertFalse(viewModel.uiState.value.sinSenal)
        assertEquals(1, viewModel.uiState.value.marcador!!.rival.puntaje)
    }

    // El ETag viaja de verdad por HTTP: tras la primera consulta, las siguientes lo mandan y, sin cambios, reciben 304.
    @Test
    fun lasConsultasSiguientesMandanElEtagYSinCambiosRecibenUn304() = runTest {
        val viewModel = crear()

        viewModel.sondear()
        viewModel.sondear()
        puntajeDelRival = 3
        viewModel.sondear()

        assertEquals(listOf<String?>(null, "\"rival-0\"", "\"rival-0\""), synchronized(consultasConEtag) { consultasConEtag.toList() })
        assertEquals(3, viewModel.uiState.value.marcador!!.rival.puntaje)
    }
}
