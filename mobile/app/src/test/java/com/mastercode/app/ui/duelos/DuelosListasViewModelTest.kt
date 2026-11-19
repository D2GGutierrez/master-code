package com.mastercode.app.ui.duelos

import com.mastercode.app.R
import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import com.mastercode.app.data.cursos.CursoInscrito
import com.mastercode.app.data.cursos.FakeMisCursosApi
import com.mastercode.app.data.cursos.MisCursosRepository
import com.mastercode.app.data.duelos.Companero
import com.mastercode.app.data.duelos.DuelosRepository
import com.mastercode.app.data.duelos.FakeDuelosApi
import com.mastercode.app.data.duelos.Invitacion
import com.mastercode.app.data.duelos.PaginaDeHistorial
import com.mastercode.app.data.duelos.PartidaDelHistorial
import com.mastercode.app.data.duelos.PuestoDelRanking
import com.mastercode.app.data.duelos.Ranking
import com.mastercode.app.data.duelos.RetoCreado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

// HU57-T3 (retar), H7 (invitaciones), HU60-T3 (historial y ranking): las pantallas de la sección Duelos.
@OptIn(ExperimentalCoroutinesApi::class)
class DuelosListasViewModelTest {

    private val api = FakeDuelosApi()
    private val cursosApi = FakeMisCursosApi()
    private val auth = AuthRepository(FakeAuthApi(), MemoriaTokenStorage(refresh = "refresh-guardado"))
    private val cursos = listOf(
        CursoInscrito(3, "Algoritmos I", "python", LocalDate.of(2026, 10, 20)),
        CursoInscrito(4, "Java Básico", "java", LocalDate.of(2026, 10, 27)),
    )

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun duelos() = DuelosRepository(api, auth)
    private fun misCursos() = MisCursosRepository(cursosApi, auth)

    // ---- Retar ----

    @Test
    fun retarMuestraLosCursosYNoPideCompanerosHastaElegir() {
        cursosApi.respuesta = { cursos }

        val estado = RetarViewModel(misCursos(), duelos()).uiState.value

        assertEquals(ContenidoDeCursos.Lista(cursos), estado.cursos)
        assertEquals(ContenidoDeCompaneros.SinCurso, estado.companeros)
        assertEquals(emptyList<Long>(), api.companerosPedidos)
    }

    // Con un solo curso no hay nada que elegir: ya vienen sus compañeros.
    @Test
    fun conUnSoloCursoYaVienenSusCompaneros() {
        cursosApi.respuesta = { listOf(cursos[0]) }
        api.alPedirCompaneros = { listOf(Companero(4, "beto")) }

        val estado = RetarViewModel(misCursos(), duelos()).uiState.value

        assertEquals(3L, estado.cursoElegido)
        assertEquals(ContenidoDeCompaneros.Lista(listOf(Companero(4, "beto"))), estado.companeros)
    }

    @Test
    fun alElegirUnCursoSeListanSusCompaneros() {
        cursosApi.respuesta = { cursos }
        api.alPedirCompaneros = { listOf(Companero(4, "beto"), Companero(9, "carla")) }
        val viewModel = RetarViewModel(misCursos(), duelos())

        viewModel.elegirCurso(4)

        assertEquals(listOf(4L), api.companerosPedidos)
        assertEquals(2, (viewModel.uiState.value.companeros as ContenidoDeCompaneros.Lista).companeros.size)
    }

    @Test
    fun unCursoSinOtrosInscritosAvisaQueNoHayAQuienRetar() {
        cursosApi.respuesta = { cursos }
        val viewModel = RetarViewModel(misCursos(), duelos())

        viewModel.elegirCurso(3)

        assertEquals(ContenidoDeCompaneros.Vacio, viewModel.uiState.value.companeros)
    }

    @Test
    fun alRetarSeCreaElRetoYSePideAbrirLaPartida() {
        cursosApi.respuesta = { cursos }
        api.alPedirCompaneros = { listOf(Companero(4, "beto")) }
        api.alRetar = { cursoId, _ -> RetoCreado(11, cursoId, "beto", 3) }
        val viewModel = RetarViewModel(misCursos(), duelos())
        viewModel.elegirCurso(3)

        viewModel.retar(Companero(4, "beto"))

        assertEquals(listOf(3L to 4L), api.retosEnviados)
        assertEquals(RetoCreado(11, 3, "beto", 3), viewModel.uiState.value.retoEnviado)
        assertNull(viewModel.uiState.value.retando)

        viewModel.alAbrirseLaPartida()
        assertNull(viewModel.uiState.value.retoEnviado)
    }

    @Test
    fun cadaRechazoDelServidorTieneSuMensaje() {
        cursosApi.respuesta = { cursos }
        val viewModel = RetarViewModel(misCursos(), duelos())
        viewModel.elegirCurso(3)
        val esperados = mapOf(
            400 to R.string.retar_error_rival,
            403 to R.string.retar_error_no_inscrito,
            409 to R.string.retar_error_ya_hay_reto,
            503 to R.string.retar_error_no_disponible,
        )

        for ((codigo, mensaje) in esperados) {
            api.alRetar = { _, _ -> throw ErrorHttp(codigo) }
            viewModel.retar(Companero(4, "beto"))
            assertEquals(mensaje, viewModel.uiState.value.error)
            assertNull(viewModel.uiState.value.retoEnviado)
        }
    }

    @Test
    fun sinSenalAlRetarAvisaYPermiteReintentar() {
        cursosApi.respuesta = { cursos }
        val viewModel = RetarViewModel(misCursos(), duelos())
        viewModel.elegirCurso(3)
        api.alRetar = { _, _ -> throw IOException("sin red") }

        viewModel.retar(Companero(4, "beto"))

        assertEquals(R.string.duelos_error_conexion, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.retando)
    }

    @Test
    fun sinCursosSeAvisa() {
        cursosApi.respuesta = { emptyList() }

        assertEquals(ContenidoDeCursos.Vacio, RetarViewModel(misCursos(), duelos()).uiState.value.cursos)
    }

    // ---- Invitaciones ----

    private val invitacion = Invitacion(11, 3, "ana", "2026-11-22T10:05:00Z")
    private val otra = Invitacion(12, 3, "carla", null)

    @Test
    fun lasInvitacionesPendientesSeListan() {
        api.alPedirInvitaciones = { listOf(invitacion, otra) }

        val estado = InvitacionesViewModel(duelos()).uiState.value

        assertEquals(ContenidoDeInvitaciones.Lista(listOf(invitacion, otra)), estado.contenido)
    }

    @Test
    fun sinInvitacionesSeAvisa() {
        assertEquals(ContenidoDeInvitaciones.Vacio, InvitacionesViewModel(duelos()).uiState.value.contenido)
    }

    @Test
    fun aceptarUnRetoAbreLaPartida() {
        api.alPedirInvitaciones = { listOf(invitacion) }
        val viewModel = InvitacionesViewModel(duelos())

        viewModel.aceptar(invitacion)

        assertEquals(listOf(11L), api.aceptadas)
        assertEquals(11L, viewModel.uiState.value.partidaAceptada)
        viewModel.alAbrirseLaPartida()
        assertNull(viewModel.uiState.value.partidaAceptada)
    }

    // 409: caducó o ya la respondió desde otro lado; se avisa y la invitación sale de la lista.
    @Test
    fun unRetoCaducadoSeAvisaYSeQuitaDeLaLista() {
        api.alPedirInvitaciones = { listOf(invitacion, otra) }
        api.alAceptar = { throw ErrorHttp(409) }
        val viewModel = InvitacionesViewModel(duelos())

        viewModel.aceptar(invitacion)

        assertEquals(R.string.invitaciones_caducada, viewModel.uiState.value.mensaje)
        assertNull(viewModel.uiState.value.partidaAceptada)
        assertEquals(ContenidoDeInvitaciones.Lista(listOf(otra)), viewModel.uiState.value.contenido)
    }

    @Test
    fun rechazarUnRetoLoQuitaDeLaLista() {
        api.alPedirInvitaciones = { listOf(invitacion) }
        val viewModel = InvitacionesViewModel(duelos())

        viewModel.rechazar(invitacion)

        assertEquals(listOf(11L), api.rechazadas)
        assertEquals(ContenidoDeInvitaciones.Vacio, viewModel.uiState.value.contenido)
    }

    @Test
    fun sinSenalAlAceptarNoPierdeLaInvitacion() {
        api.alPedirInvitaciones = { listOf(invitacion) }
        api.alAceptar = { throw IOException("sin red") }
        val viewModel = InvitacionesViewModel(duelos())

        viewModel.aceptar(invitacion)

        assertEquals(R.string.duelos_error_conexion, viewModel.uiState.value.mensaje)
        assertEquals(ContenidoDeInvitaciones.Lista(listOf(invitacion)), viewModel.uiState.value.contenido)
    }

    // ---- Historial ----

    private fun partida(id: Long) = PartidaDelHistorial(id, 3, "terminada", "beto", 3, 1, "gane", "completada", "2026-11-22T10:00:00Z")

    @Test
    fun elHistorialTraeLaPrimeraPaginaYPideMasCuandoHay() {
        api.alPedirHistorial = { pagina, tamano ->
            if (pagina == 1) PaginaDeHistorial((1L..20L).map(::partida), 1, tamano, 25)
            else PaginaDeHistorial((21L..25L).map(::partida), 2, tamano, 25)
        }
        val viewModel = HistorialViewModel(duelos())
        assertEquals(20, viewModel.uiState.value.partidas.size)
        assertTrue(viewModel.uiState.value.hayMas)

        viewModel.cargarMas()

        assertEquals(25, viewModel.uiState.value.partidas.size)
        assertFalse(viewModel.uiState.value.hayMas)
        assertEquals(listOf(1 to 20, 2 to 20), api.historialesPedidos)

        // Ya no hay más páginas: no vuelve a pedir.
        viewModel.cargarMas()
        assertEquals(2, api.historialesPedidos.size)
    }

    @Test
    fun sinPartidasElHistorialEstaVacio() {
        assertTrue(HistorialViewModel(duelos()).uiState.value.vacio)
    }

    @Test
    fun siFallaUnaPaginaSeConservaLoYaTraido() {
        api.alPedirHistorial = { pagina, tamano ->
            if (pagina == 1) PaginaDeHistorial((1L..20L).map(::partida), 1, tamano, 40) else throw IOException("sin red")
        }
        val viewModel = HistorialViewModel(duelos())

        viewModel.cargarMas()

        assertEquals(20, viewModel.uiState.value.partidas.size)
        assertEquals(R.string.duelos_error_conexion, viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.hayMas)
    }

    // ---- Ranking ----

    private fun puesto(posicion: Int, alias: String, yo: Boolean = false) =
        PuestoDelRanking(posicion, alias, 4, 2, 1, 1, 8, 50.0, yo)

    @Test
    fun elRankingMuestraLasFilasYResaltaMiPuesto() {
        cursosApi.respuesta = { listOf(cursos[0]) }
        val filas = listOf(puesto(1, "ana"), puesto(2, "beto", yo = true))
        api.alPedirRanking = { cursoId, pagina, tamano -> Ranking(cursoId, 2, pagina, tamano, filas, filas[1]) }

        val estado = RankingViewModel(misCursos(), duelos()).uiState.value

        assertEquals(filas, estado.puestos)
        assertTrue(estado.puestos.single { it.soyYo }.alias == "beto")
        assertNull(estado.miPuestoFueraDeLaLista)
    }

    // Si mi fila está en otra página, el ranking la muestra aparte (HU60-T3, CA-60.3).
    @Test
    fun miPuestoSeMuestraAparteSiNoEstaEnLaPaginaCargada() {
        cursosApi.respuesta = { listOf(cursos[0]) }
        val mio = puesto(30, "yo", yo = true)
        api.alPedirRanking = { cursoId, pagina, tamano ->
            Ranking(cursoId, 40, pagina, tamano, (1..20).map { puesto(it, "j$it") }, mio)
        }

        val estado = RankingViewModel(misCursos(), duelos()).uiState.value

        assertEquals(mio, estado.miPuestoFueraDeLaLista)
        assertTrue(estado.hayMas)
    }

    // HU60-T4: quien no jugó en el curso sale al final con ceros; la pantalla lo avisa y el resto del ranking se ve igual.
    @Test
    fun sinPartidasElRankingLoDiceYSigueMostrandoALosDemas() {
        cursosApi.respuesta = { listOf(cursos[0]) }
        val mio = PuestoDelRanking(2, "yo", 0, 0, 0, 0, 0, 0.0, true)
        api.alPedirRanking = { cursoId, pagina, tamano ->
            Ranking(cursoId, 2, pagina, tamano, listOf(puesto(1, "ana"), mio), mio)
        }

        val estado = RankingViewModel(misCursos(), duelos()).uiState.value

        assertTrue(estado.sinPartidas)
        assertEquals(2, estado.puestos.size)
        assertNull(estado.miPuestoFueraDeLaLista)
    }

    // Si user-api no manda mi puesto, también cuenta como «sin partidas».
    @Test
    fun sinMiPuestoTambienCuentaComoSinPartidas() {
        cursosApi.respuesta = { listOf(cursos[0]) }

        val estado = RankingViewModel(misCursos(), duelos()).uiState.value

        assertTrue(estado.sinPartidas)
        assertNull(estado.yo)
    }

    @Test
    fun quienYaJugoNoVeElAvisoDeSinPartidas() {
        cursosApi.respuesta = { listOf(cursos[0]) }
        val mio = puesto(1, "yo", yo = true)
        api.alPedirRanking = { cursoId, pagina, tamano -> Ranking(cursoId, 1, pagina, tamano, listOf(mio), mio) }

        assertFalse(RankingViewModel(misCursos(), duelos()).uiState.value.sinPartidas)
    }

    // HU60-T4: si el curso no es del estudiante, user-api contesta 403.
    @Test
    fun unCursoAjenoDaElMensajeDeNoInscrito() {
        cursosApi.respuesta = { cursos }
        api.alPedirRanking = { _, _, _ -> throw ErrorHttp(403) }
        val viewModel = RankingViewModel(misCursos(), duelos())

        viewModel.elegirCurso(99)

        assertEquals(R.string.ranking_error_no_inscrito, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.sinPartidas)
    }

    @Test
    fun alCambiarDeCursoElRankingEmpiezaDeNuevo() {
        cursosApi.respuesta = { cursos }
        api.alPedirRanking = { cursoId, pagina, tamano ->
            Ranking(cursoId, 1, pagina, tamano, listOf(puesto(1, "curso$cursoId")), null)
        }
        val viewModel = RankingViewModel(misCursos(), duelos())

        viewModel.elegirCurso(3)
        viewModel.elegirCurso(4)

        assertEquals(listOf("curso4"), viewModel.uiState.value.puestos.map { it.alias })
        assertEquals(listOf(Triple(3L, 1, 20), Triple(4L, 1, 20)), api.rankingsPedidos)
    }
}
