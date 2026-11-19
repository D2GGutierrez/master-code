package com.mastercode.app.ui.duelos

import com.mastercode.app.R
import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import com.mastercode.app.data.duelos.DuelosRepository
import com.mastercode.app.data.duelos.EjercicioDelTurno
import com.mastercode.app.data.duelos.FakeDuelosApi
import com.mastercode.app.data.duelos.FakeDuelosApi.Companion.ejercicioDe
import com.mastercode.app.data.duelos.FakeDuelosApi.Companion.marcadorDe
import com.mastercode.app.data.duelos.LecturaDeMarcador
import com.mastercode.app.data.duelos.RespuestaDePartida
import com.mastercode.app.ui.ejercicios.EstadoDeEnvio
import com.mastercode.app.ui.ejercicios.TIPO_OPCION_MULTIPLE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

// H8 y HU59-T3: la partida paso a paso. Cada prueba llama a sondear() cuando quiere que pase una consulta del marcador de 2 s;
// el sondeo automático (reanudar y pausar) se prueba al final con una pausa que controla la prueba.
@OptIn(ExperimentalCoroutinesApi::class)
class PartidaViewModelTest {

    private val api = FakeDuelosApi()
    private val auth = AuthRepository(FakeAuthApi(), MemoriaTokenStorage(refresh = "refresh-guardado"))

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crear() = PartidaViewModel(DuelosRepository(api, auth), 7)

    private fun PartidaViewModel.jugando() = uiState.value.fase as FaseDePartida.Jugando

    // El servidor entrega los ejercicios de la lista, uno por turno, y null (204) cuando se acaban.
    private fun conEjercicios(vararg turnos: EjercicioDelTurno) {
        val cola = turnos.toMutableList()
        api.alPedirEjercicio = { if (cola.isEmpty()) null else cola.removeAt(0) }
    }

    @Test
    fun alAbrirLaPartidaEmpiezaCargando() {
        assertEquals(FaseDePartida.Cargando, crear().uiState.value.fase)
    }

    // El retador: el reto sigue pendiente hasta que el rival acepta, y entonces aparece el primer ejercicio.
    @Test
    fun elRetadorEsperaAlRivalYAlAceptarJuegaElPrimerTurno() = runTest {
        var estado = "pendiente"
        api.alPedirMarcador = { LecturaDeMarcador(marcadorDe(it, estado = estado), "\"$estado\"") }
        conEjercicios(ejercicioDe(1))
        val viewModel = crear()

        viewModel.sondear()
        assertEquals(FaseDePartida.EsperandoRival, viewModel.uiState.value.fase)

        estado = "en_curso"
        viewModel.sondear()
        assertEquals(1, viewModel.jugando().turno)
        assertEquals(3, viewModel.jugando().total)
    }

    // Quien aceptó: la partida ya está en curso y va directo al ejercicio.
    @Test
    fun quienAceptoVaDirectoAlPrimerEjercicio() = runTest {
        conEjercicios(ejercicioDe(1))
        val viewModel = crear()

        viewModel.sondear()

        assertEquals(1, viewModel.jugando().turno)
        assertEquals("beto", viewModel.uiState.value.marcador!!.rival.alias)
    }

    // H8: se responde turno a turno; la corrección y el marcador salen al instante, sin esperar la consulta de 2 s.
    @Test
    fun respondeUnTurnoYVeLaCorreccionYElMarcadorNuevo() = runTest {
        conEjercicios(ejercicioDe(1), ejercicioDe(2))
        api.alResponder = { id, _, _, _ -> RespuestaDePartida(true, null, marcadorDe(id, yo = 1, respondidosYo = 1)) }
        val viewModel = crear()
        viewModel.sondear()

        val ejercicio = viewModel.jugando().ejercicio
        ejercicio.alCambiarRespuesta("4")
        ejercicio.enviar()

        assertEquals(EstadoDeEnvio.Resultado(true, null), ejercicio.uiState.value.envio)
        assertEquals(listOf(FakeDuelosApi.RespuestaEnviada(7, 1, "4", null)), api.respuestasEnviadas)
        assertEquals(1, viewModel.uiState.value.marcador!!.yo.puntaje)
        assertTrue(viewModel.uiState.value.respondido)
    }

    @Test
    fun unaRespuestaFallidaMuestraLaCorrectaYNoSumaPuntos() = runTest {
        conEjercicios(ejercicioDe(1))
        api.alResponder = { id, _, _, _ -> RespuestaDePartida(false, "5", marcadorDe(id, respondidosYo = 1)) }
        val viewModel = crear()
        viewModel.sondear()

        val ejercicio = viewModel.jugando().ejercicio
        ejercicio.alCambiarRespuesta("4")
        ejercicio.enviar()

        assertEquals(EstadoDeEnvio.Resultado(false, "5"), ejercicio.uiState.value.envio)
        assertEquals(0, viewModel.uiState.value.marcador!!.yo.puntaje)
    }

    // Los ejercicios de opción múltiple mandan la alternativa elegida, no texto.
    @Test
    fun enOpcionMultipleSeEnviaLaAlternativaElegida() = runTest {
        conEjercicios(ejercicioDe(1, tipo = TIPO_OPCION_MULTIPLE))
        val viewModel = crear()
        viewModel.sondear()

        val ejercicio = viewModel.jugando().ejercicio
        ejercicio.alElegirOpcion(9)
        ejercicio.enviar()

        assertEquals(listOf(FakeDuelosApi.RespuestaEnviada(7, 1, null, 9)), api.respuestasEnviadas)
    }

    @Test
    fun noSePuedeEnviarSinResponderNiDosVeces() = runTest {
        conEjercicios(ejercicioDe(1))
        val viewModel = crear()
        viewModel.sondear()
        val ejercicio = viewModel.jugando().ejercicio

        ejercicio.enviar()
        assertEquals(emptyList<FakeDuelosApi.RespuestaEnviada>(), api.respuestasEnviadas)

        ejercicio.alCambiarRespuesta("4")
        ejercicio.enviar()
        ejercicio.enviar()
        assertEquals(1, api.respuestasEnviadas.size)
    }

    @Test
    fun siguienteTraeElEjercicioDelTurnoQueSigue() = runTest {
        conEjercicios(ejercicioDe(1), ejercicioDe(2))
        val viewModel = crear()
        viewModel.sondear()
        viewModel.jugando().ejercicio.apply { alCambiarRespuesta("4"); enviar() }

        viewModel.siguiente()

        assertEquals(2, viewModel.jugando().turno)
    }

    // Sin más ejercicios (204) el estudiante espera a que el rival termine; cuando termina, ve el resultado.
    @Test
    fun alTerminarSusEjerciciosEsperaAlRivalYLuegoVeElResultado() = runTest {
        conEjercicios(ejercicioDe(1, total = 1))
        val viewModel = crear()
        viewModel.sondear()
        viewModel.jugando().ejercicio.apply { alCambiarRespuesta("4"); enviar() }
        viewModel.siguiente()
        assertEquals(FaseDePartida.EsperandoAlRival, viewModel.uiState.value.fase)

        api.alPedirMarcador = {
            LecturaDeMarcador(marcadorDe(it, estado = "terminada", yo = 1, rival = 0, resultado = "gane", motivoFin = "completada"), "\"fin\"")
        }
        viewModel.sondear()

        val terminada = viewModel.uiState.value.fase as FaseDePartida.Terminada
        assertEquals("gane", terminada.marcador.resultado)
    }

    // HU59-T3: lo que avanza el rival se ve en la consulta siguiente; si no cambió (304) no se toca nada.
    @Test
    fun elMarcadorDelRivalSeActualizaConLaConsultaYConUn304NoCambiaNada() = runTest {
        var rival = 0
        api.alPedirMarcador = { LecturaDeMarcador(marcadorDe(it, rival = rival, respondidosRival = rival), "\"r$rival\"") }
        conEjercicios(ejercicioDe(1))
        val viewModel = crear()
        viewModel.sondear()
        assertEquals(0, viewModel.uiState.value.marcador!!.rival.puntaje)

        viewModel.sondear()
        assertEquals(listOf(null, "\"r0\""), api.etagsEnviados)
        assertEquals(0, viewModel.uiState.value.marcador!!.rival.puntaje)

        rival = 2
        viewModel.sondear()

        assertEquals(2, viewModel.uiState.value.marcador!!.rival.puntaje)
        assertEquals(listOf(null, "\"r0\"", "\"r0\""), api.etagsEnviados)
    }

    @Test
    fun siElRivalPierdeLaSenalSeVeEnElMarcador() = runTest {
        var conectado = true
        api.alPedirMarcador = { LecturaDeMarcador(marcadorDe(it, rivalConectado = conectado), "\"c$conectado\"") }
        conEjercicios(ejercicioDe(1))
        val viewModel = crear()
        viewModel.sondear()
        assertTrue(viewModel.uiState.value.marcador!!.rivalConectado)

        conectado = false
        viewModel.sondear()

        assertFalse(viewModel.uiState.value.marcador!!.rivalConectado)
    }

    // HU59-T5: sin señal o con la consulta caída, el marcador y el ejercicio siguen en pantalla y se avisa que se reintenta.
    @Test
    fun sinSenalConservaElMarcadorYElEjercicioYAvisa() = runTest {
        conEjercicios(ejercicioDe(1))
        val viewModel = crear()
        viewModel.sondear()
        val antes = viewModel.uiState.value

        api.alPedirMarcador = { throw IOException("sin red") }
        viewModel.sondear()

        val durante = viewModel.uiState.value
        assertTrue(durante.sinSenal)
        assertEquals(antes.marcador, durante.marcador)
        assertEquals(antes.fase, durante.fase)
    }

    @Test
    fun cuandoVuelveLaSenalSeQuitaElAviso() = runTest {
        conEjercicios(ejercicioDe(1))
        val viewModel = crear()
        viewModel.sondear()
        api.alPedirMarcador = { throw IOException("sin red") }
        viewModel.sondear()
        assertTrue(viewModel.uiState.value.sinSenal)

        api.alPedirMarcador = { LecturaDeMarcador(marcadorDe(it, rival = 1), "\"nuevo\"") }
        viewModel.sondear()

        assertFalse(viewModel.uiState.value.sinSenal)
        assertEquals(1, viewModel.uiState.value.marcador!!.rival.puntaje)
    }

    // Un 503 al consultar el marcador es pasajero: no cierra la partida.
    @Test
    fun unErrorPasajeroDelServidorNoCierraLaPartida() = runTest {
        conEjercicios(ejercicioDe(1))
        val viewModel = crear()
        viewModel.sondear()
        api.alPedirMarcador = { throw ErrorHttp(503) }

        viewModel.sondear()

        assertTrue(viewModel.uiState.value.sinSenal)
        assertTrue(viewModel.uiState.value.fase is FaseDePartida.Jugando)
    }

    @Test
    fun siLaRespuestaNoLlegaPorFaltaDeSenalPuedeVolverAEnviarse() = runTest {
        conEjercicios(ejercicioDe(1))
        val viewModel = crear()
        viewModel.sondear()
        val ejercicio = viewModel.jugando().ejercicio
        ejercicio.alCambiarRespuesta("4")
        api.alResponder = { _, _, _, _ -> throw IOException("sin red") }

        ejercicio.enviar()

        assertEquals(EstadoDeEnvio.Rechazado(R.string.partida_error_conexion_respuesta), ejercicio.uiState.value.envio)
        assertTrue(ejercicio.uiState.value.editable)

        api.alResponder = { id, _, _, _ -> RespuestaDePartida(true, null, marcadorDe(id, yo = 1, respondidosYo = 1)) }
        ejercicio.enviar()

        assertEquals(EstadoDeEnvio.Resultado(true, null), ejercicio.uiState.value.envio)
    }

    // Si la respuesta sí llegó pero se perdió la contestación, reenviar da 409: se pasa al turno que user-api espera.
    @Test
    fun siElTurnoYaEstabaRespondidoSePasaAlQueSigue() = runTest {
        conEjercicios(ejercicioDe(1), ejercicioDe(2))
        val viewModel = crear()
        viewModel.sondear()
        val ejercicio = viewModel.jugando().ejercicio
        ejercicio.alCambiarRespuesta("4")
        api.alResponder = { _, _, _, _ -> throw ErrorHttp(409) }

        ejercicio.enviar()

        assertEquals(EstadoDeEnvio.Rechazado(R.string.partida_turno_ya_respondido), ejercicio.uiState.value.envio)
        assertEquals(2, viewModel.jugando().turno)
    }

    @Test
    fun unaRespuestaInvalidaDejaCorregirYReenviar() = runTest {
        conEjercicios(ejercicioDe(1))
        val viewModel = crear()
        viewModel.sondear()
        val ejercicio = viewModel.jugando().ejercicio
        ejercicio.alCambiarRespuesta("   x")
        api.alResponder = { _, _, _, _ -> throw ErrorHttp(400) }

        ejercicio.enviar()

        assertEquals(EstadoDeEnvio.Rechazado(R.string.partida_respuesta_invalida), ejercicio.uiState.value.envio)
        assertTrue(ejercicio.uiState.value.puedeEnviar)
    }

    @Test
    fun siNoSePuedeCargarElEjercicioSeOfreceReintentar() = runTest {
        var cae = true
        api.alPedirEjercicio = { if (cae) throw IOException("sin red") else ejercicioDe(1) }
        val viewModel = crear()

        viewModel.sondear()
        assertEquals(FaseDePartida.Error(R.string.partida_error_conexion), viewModel.uiState.value.fase)

        cae = false
        viewModel.reintentar()

        assertEquals(1, viewModel.jugando().turno)
    }

    // El rival abandona (user-api la cierra) mientras el estudiante todavía no respondió: ve el resultado enseguida.
    @Test
    fun siElRivalAbandonaSeCierraLaPartidaConElResultado() = runTest {
        conEjercicios(ejercicioDe(1))
        val viewModel = crear()
        viewModel.sondear()
        api.alPedirMarcador = {
            LecturaDeMarcador(marcadorDe(it, estado = "terminada", resultado = "gane", motivoFin = "abandono"), "\"fin\"")
        }

        viewModel.sondear()

        val terminada = viewModel.uiState.value.fase as FaseDePartida.Terminada
        assertEquals("abandono", terminada.marcador.motivoFin)
    }

    // Si estaba mirando la corrección de su última respuesta, el resultado espera a que toque «Siguiente».
    @Test
    fun siTerminaMientrasVeLaCorreccionElResultadoEsperaAlSiguiente() = runTest {
        conEjercicios(ejercicioDe(1, total = 1))
        val viewModel = crear()
        viewModel.sondear()
        viewModel.jugando().ejercicio.apply { alCambiarRespuesta("4"); enviar() }
        api.alPedirMarcador = {
            LecturaDeMarcador(marcadorDe(it, estado = "terminada", yo = 1, resultado = "gane", motivoFin = "completada"), "\"fin\"")
        }

        viewModel.sondear()
        assertTrue(viewModel.uiState.value.fase is FaseDePartida.Jugando)

        viewModel.siguiente()
        assertTrue(viewModel.uiState.value.fase is FaseDePartida.Terminada)
    }

    @Test
    fun unRetoRechazadoOCaducadoCierraLaPartidaConSuMensaje() = runTest {
        val rechazado = crear()
        api.alPedirMarcador = { LecturaDeMarcador(marcadorDe(it, estado = "rechazada"), "\"x\"") }
        rechazado.sondear()
        assertEquals(FaseDePartida.Cerrada(R.string.partida_rechazada), rechazado.uiState.value.fase)

        val caducado = crear()
        api.alPedirMarcador = { LecturaDeMarcador(marcadorDe(it, estado = "caducada"), "\"y\"") }
        caducado.sondear()
        assertEquals(FaseDePartida.Cerrada(R.string.partida_caducada), caducado.uiState.value.fase)
    }

    @Test
    fun unaPartidaAjenaOInexistenteNoSeAbre() = runTest {
        api.alPedirMarcador = { throw ErrorHttp(404) }
        val viewModel = crear()

        viewModel.sondear()

        assertEquals(FaseDePartida.Cerrada(R.string.partida_no_existe), viewModel.uiState.value.fase)
        assertNull(viewModel.uiState.value.marcador)
    }

    @Test
    fun conLaSesionVencidaSePideIniciarSesion() = runTest {
        val sinSesion = AuthRepository(FakeAuthApi(), MemoriaTokenStorage(refresh = null))
        val viewModel = PartidaViewModel(DuelosRepository(api, sinSesion), 7)

        viewModel.sondear()

        assertEquals(FaseDePartida.SesionVencida, viewModel.uiState.value.fase)
    }

    // ---------- el sondeo cada 2 s: se enciende al ver la pantalla y se apaga al salir ----------

    // La pausa entre consultas la da la prueba: cada tic equivale a que pasaron los 2 s.
    private fun conPausaManual(): Pair<PartidaViewModel, kotlinx.coroutines.channels.Channel<Unit>> {
        val tics = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        return PartidaViewModel(DuelosRepository(api, auth), 7, esperar = { tics.receive() }) to tics
    }

    @Test
    fun alReanudarConsultaElMarcadorYLoRepiteConCadaPausa() {
        conEjercicios(ejercicioDe(1))
        val (viewModel, tics) = conPausaManual()

        viewModel.reanudar()
        assertEquals(1, api.etagsEnviados.size)

        tics.trySend(Unit)
        tics.trySend(Unit)

        assertEquals(3, api.etagsEnviados.size)
        // Desde la segunda consulta ya manda el ETag de la anterior.
        assertEquals(listOf(null, "\"1\"", "\"1\""), api.etagsEnviados)
    }

    // Al salir de la pantalla (o pasar la app a segundo plano) deja de dar señal: así user-api puede dar la partida por abandonada.
    @Test
    fun alPausarDejaDeConsultarElMarcador() {
        conEjercicios(ejercicioDe(1))
        val (viewModel, tics) = conPausaManual()
        viewModel.reanudar()
        tics.trySend(Unit)
        assertEquals(2, api.etagsEnviados.size)

        viewModel.pausar()
        tics.trySend(Unit)
        tics.trySend(Unit)

        assertEquals(2, api.etagsEnviados.size)
    }

    @Test
    fun alVolverAReanudarConsultaDeNuevoYNoDuplicaElSondeo() {
        conEjercicios(ejercicioDe(1))
        val (viewModel, tics) = conPausaManual()
        viewModel.reanudar()
        viewModel.reanudar()
        assertEquals(1, api.etagsEnviados.size)
        viewModel.pausar()

        viewModel.reanudar()
        assertEquals(2, api.etagsEnviados.size)
        tics.trySend(Unit)

        assertEquals(3, api.etagsEnviados.size)
    }

    @Test
    fun cuandoLaPartidaTerminaElSondeoSeDetiene() {
        conEjercicios(ejercicioDe(1))
        api.alPedirMarcador = {
            LecturaDeMarcador(marcadorDe(it, estado = "terminada", resultado = "gane", motivoFin = "completada"), "\"fin\"")
        }
        val (viewModel, tics) = conPausaManual()

        viewModel.reanudar()
        tics.trySend(Unit)
        tics.trySend(Unit)

        assertTrue(viewModel.uiState.value.fase is FaseDePartida.Terminada)
        assertEquals(1, api.etagsEnviados.size)
        // Una partida terminada no vuelve a consultar aunque se reanude.
        viewModel.reanudar()
        assertEquals(1, api.etagsEnviados.size)
    }
}
