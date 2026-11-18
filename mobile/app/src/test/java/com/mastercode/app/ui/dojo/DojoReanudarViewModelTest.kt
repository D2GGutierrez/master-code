package com.mastercode.app.ui.dojo

import com.mastercode.app.R
import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import com.mastercode.app.data.dojo.DojoRepository
import com.mastercode.app.data.dojo.EstadoDeSesionServidor
import com.mastercode.app.data.dojo.FakeDojoApi
import com.mastercode.app.data.dojo.ResumenDeSesion
import com.mastercode.app.data.problemas.ProblemaDelEjercicio
import com.mastercode.app.data.respuestas.EstadoDeSesion
import com.mastercode.app.data.respuestas.MemoriaSesion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

// HU55-T2 (CA-55.2): al volver al Dojo se ofrece continuar donde quedó. HU55-T3 (CA-55.3, CA-55.4): «Terminar sesión» con
// confirmación y resumen. HU55-T4 (CA-55.5): cerrar la app a media sesión y volver a abrirla (otro DojoViewModel con la misma
// memoria guardada, que es lo que pasa al matar la app y abrirla de nuevo).
@OptIn(ExperimentalCoroutinesApi::class)
class DojoReanudarViewModelTest {

    private val primero = ProblemaDelEjercicio(12, "Suma", "¿Qué imprime?", "prediccion_salida", intentosRestantes = 3)
    private val segundo = ProblemaDelEjercicio(13, "Resta", "¿Qué imprime?", "prediccion_salida", intentosRestantes = 3)
    private val guardada = EstadoDeSesion(5, "python", 12, 1, 2)
    private val resumen = ResumenDeSesion("medio", 4, 3, 30, emptyList(), 1, 30)

    private val api = FakeDojoApi()
    private val almacen = MemoriaSesion()
    private val auth = AuthRepository(FakeAuthApi(), MemoriaTokenStorage(refresh = "refresh-guardado"))

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crear() = DojoViewModel(DojoRepository(api, auth), almacen)

    private fun conSesionGuardada(): DojoViewModel {
        almacen.estado = guardada
        return crear()
    }

    // Lo que user-api contesta de la sesión 5, abierta.
    private fun servidorDice(aciertos: Int, respondidos: Int, ejercicio: ProblemaDelEjercicio?) {
        api.alConsultar = { EstadoDeSesionServidor(it, "python", true, aciertos, respondidos, ejercicio) }
    }

    // ---------- HU55-T2: continuar donde quedó ----------

    @Test
    fun sinSesionGuardadaSeEmpiezaEligiendoLenguaje() {
        assertEquals(FaseDelDojo.Eligiendo, crear().uiState.value.fase)
    }

    @Test
    fun conUnaSesionGuardadaSeOfreceContinuarSinLlamarAlServidor() {
        val viewModel = conSesionGuardada()

        assertEquals(FaseDelDojo.OfreciendoReanudar(guardada), viewModel.uiState.value.fase)
        assertNull(viewModel.uiState.value.sesionId)
        assertEquals(emptyList<Long>(), api.sesionesConsultadas)
        assertEquals(emptyList<String>(), api.lenguajesPedidos)
    }

    // Mientras decide, salir de la pantalla no pisa lo guardado.
    @Test
    fun salirMientrasSeOfreceNoCambiaLoGuardado() {
        val viewModel = conSesionGuardada()

        viewModel.alSalir()

        assertEquals(guardada, almacen.estado)
        assertEquals(emptyList<EstadoDeSesion?>(), almacen.guardados)
    }

    @Test
    fun elegirLenguajeConUnaOfertaPendienteSeIgnora() {
        val viewModel = conSesionGuardada()

        viewModel.elegirLenguaje("java")

        assertEquals(emptyList<String>(), api.lenguajesPedidos)
        assertEquals(FaseDelDojo.OfreciendoReanudar(guardada), viewModel.uiState.value.fase)
    }

    // CA-55.2: se muestra el ejercicio que se dejó sin responder, con los números del servidor.
    @Test
    fun continuarMuestraElEjercicioPendienteConLosContadoresDelServidor() {
        servidorDice(aciertos = 2, respondidos = 3, ejercicio = primero)
        val viewModel = conSesionGuardada()

        viewModel.reanudar()

        val estado = viewModel.uiState.value
        assertEquals(listOf(5L), api.sesionesConsultadas)
        assertEquals(FaseDelDojo.Ejercicio(primero), estado.fase)
        assertEquals(5L, estado.sesionId)
        assertEquals("python", estado.lenguaje)
        assertEquals(2, estado.aciertos)
        assertEquals(3, estado.respondidos)
        assertFalse(estado.respondido)
        // No se pidió un ejercicio nuevo: se retoma el mismo.
        assertEquals(emptyList<Long>(), api.sesionesPedidas)
    }

    @Test
    fun continuarSinEjercicioPendientePideElSiguiente() {
        servidorDice(aciertos = 1, respondidos = 2, ejercicio = null)
        api.ejercicios = mutableListOf(segundo)
        val viewModel = conSesionGuardada()

        viewModel.reanudar()

        assertEquals(FaseDelDojo.Ejercicio(segundo), viewModel.uiState.value.fase)
        assertEquals(listOf(5L), api.sesionesPedidas)
        assertEquals(1, viewModel.uiState.value.aciertos)
        assertEquals(2, viewModel.uiState.value.respondidos)
    }

    @Test
    fun continuarSinEjercicioPendienteNiMasEjerciciosLoDice() {
        servidorDice(aciertos = 1, respondidos = 1, ejercicio = null)
        val viewModel = conSesionGuardada()

        viewModel.reanudar()

        assertEquals(FaseDelDojo.SinMasEjercicios, viewModel.uiState.value.fase)
    }

    @Test
    fun alContinuarSeGuardaElEjercicioQueSeRetoma() {
        servidorDice(aciertos = 2, respondidos = 3, ejercicio = segundo)
        val viewModel = conSesionGuardada()

        viewModel.reanudar()

        assertEquals(EstadoDeSesion(5, "python", 13, 2, 3), almacen.estado)
    }

    @Test
    fun despuesDeContinuarSeSigueRespondiendoYPidiendoEjercicios() {
        servidorDice(aciertos = 2, respondidos = 3, ejercicio = primero)
        api.ejercicios = mutableListOf(segundo)
        val viewModel = conSesionGuardada()
        viewModel.reanudar()

        viewModel.alResponder(12, acerto = true)
        viewModel.siguiente()

        assertEquals(3, viewModel.uiState.value.aciertos)
        assertEquals(4, viewModel.uiState.value.respondidos)
        assertEquals(FaseDelDojo.Ejercicio(segundo), viewModel.uiState.value.fase)
        assertEquals(EstadoDeSesion(5, "python", 13, 3, 4), almacen.estado)
    }

    // Una sesión que ya cerró (en el servidor) no se retoma: se borra lo guardado y se avisa.
    @Test
    fun continuarUnaSesionYaCerradaBorraLoGuardadoYAvisa() {
        api.alConsultar = { EstadoDeSesionServidor(it, "python", false, 3, 3, null) }
        val viewModel = conSesionGuardada()

        viewModel.reanudar()

        assertEquals(FaseDelDojo.Error(R.string.dojo_error_sesion), viewModel.uiState.value.fase)
        assertNull(almacen.estado)
        assertNull(viewModel.uiState.value.sesionId)
        assertEquals(emptyList<Long>(), api.sesionesPedidas)
    }

    @Test
    fun continuarUnaSesionQueYaNoExisteBorraLoGuardadoYAvisa() {
        api.alConsultar = { throw ErrorHttp(404) }
        val viewModel = conSesionGuardada()

        viewModel.reanudar()

        assertEquals(FaseDelDojo.Error(R.string.dojo_error_sesion), viewModel.uiState.value.fase)
        assertNull(almacen.estado)
    }

    // Tras ese aviso, «Reintentar» lleva a elegir lenguaje y se puede empezar otra sesión.
    @Test
    fun despuesDeUnaSesionPerdidaSePuedeEmpezarOtra() {
        api.alConsultar = { throw ErrorHttp(404) }
        api.ejercicios = mutableListOf(primero)
        val viewModel = conSesionGuardada()
        viewModel.reanudar()

        viewModel.alReintentar()
        viewModel.elegirLenguaje("java")

        assertEquals(listOf("java"), api.lenguajesPedidos)
        assertEquals(FaseDelDojo.Ejercicio(primero), viewModel.uiState.value.fase)
    }

    // Sin señal no se pierde la sesión: sigue la oferta con un aviso y se puede reintentar.
    @Test
    fun continuarSinConexionSigueOfreciendoYSePuedeReintentar() {
        api.alConsultar = { throw IOException("sin red") }
        val viewModel = conSesionGuardada()

        viewModel.reanudar()

        assertEquals(FaseDelDojo.OfreciendoReanudar(guardada, R.string.dojo_error_conexion), viewModel.uiState.value.fase)
        assertEquals(guardada, almacen.estado)

        servidorDice(aciertos = 1, respondidos = 2, ejercicio = primero)
        viewModel.reanudar()

        assertEquals(FaseDelDojo.Ejercicio(primero), viewModel.uiState.value.fase)
    }

    @Test
    fun continuarConUnErrorDelServidorSigueOfreciendoConElAvisoInesperado() {
        api.alConsultar = { throw ErrorHttp(503) }
        val viewModel = conSesionGuardada()

        viewModel.reanudar()

        assertEquals(FaseDelDojo.OfreciendoReanudar(guardada, R.string.dojo_error_inesperado), viewModel.uiState.value.fase)
        assertEquals(guardada, almacen.estado)
    }

    @Test
    fun continuarConLaSesionVencidaPideIniciarSesionYConservaLoGuardado() {
        val auth401 = FakeAuthApi().apply { respuestaRenovar = { throw ErrorHttp(401) } }
        val viewModel = DojoViewModel(
            DojoRepository(api, AuthRepository(auth401, MemoriaTokenStorage(refresh = "vencido"))),
            almacen.apply { estado = guardada },
        )

        viewModel.reanudar()

        assertEquals(FaseDelDojo.SesionVencida, viewModel.uiState.value.fase)
        assertEquals(guardada, almacen.estado)
    }

    @Test
    fun continuarSinOfertaNoHaceNada() {
        val viewModel = crear()

        viewModel.reanudar()

        assertEquals(FaseDelDojo.Eligiendo, viewModel.uiState.value.fase)
        assertEquals(emptyList<Long>(), api.sesionesConsultadas)
    }

    // «Empezar de nuevo»: se termina la anterior (queda guardado su nivel) y se vuelve a elegir lenguaje.
    @Test
    fun empezarDeNuevoTerminaLaSesionAnteriorBorraLoGuardadoYPideLenguaje() {
        val viewModel = conSesionGuardada()

        viewModel.empezarDeNuevo()

        assertEquals(listOf(5L), api.sesionesTerminadas)
        assertNull(almacen.estado)
        assertEquals(FaseDelDojo.Eligiendo, viewModel.uiState.value.fase)
        assertEquals(emptyList<Long>(), api.sesionesPedidas)
    }

    @Test
    fun empezarDeNuevoConLaSesionYaTerminadaTambienSigue() {
        api.alTerminar = { throw ErrorHttp(409) }
        val viewModel = conSesionGuardada()

        viewModel.empezarDeNuevo()

        assertNull(almacen.estado)
        assertEquals(FaseDelDojo.Eligiendo, viewModel.uiState.value.fase)
    }

    @Test
    fun empezarDeNuevoSinConexionNoDescartaLaSesion() {
        api.alTerminar = { throw IOException("sin red") }
        val viewModel = conSesionGuardada()

        viewModel.empezarDeNuevo()

        assertEquals(FaseDelDojo.OfreciendoReanudar(guardada, R.string.dojo_error_conexion), viewModel.uiState.value.fase)
        assertEquals(guardada, almacen.estado)
    }

    @Test
    fun despuesDeEmpezarDeNuevoSeIniciaOtraSesion() {
        api.ejercicios = mutableListOf(segundo)
        api.alIniciar = { com.mastercode.app.data.dojo.SesionIniciada(9, it, "facil") }
        val viewModel = conSesionGuardada()
        viewModel.empezarDeNuevo()

        viewModel.elegirLenguaje("java")

        assertEquals(9L, viewModel.uiState.value.sesionId)
        assertEquals(EstadoDeSesion(9, "java", 13, 0, 0), almacen.estado)
    }

    // ---------- HU55-T3: terminar la sesión ----------

    private fun conSesionEnMarcha(): DojoViewModel {
        api.ejercicios = mutableListOf(primero, segundo)
        val viewModel = crear()
        viewModel.elegirLenguaje("python")
        return viewModel
    }

    // CA-55.3: primero se pide confirmación y no se toca el servidor.
    @Test
    fun terminarPidePrimeroConfirmacionSinLlamarAlServidor() {
        val viewModel = conSesionEnMarcha()

        viewModel.pedirTerminar()

        assertEquals(true, viewModel.uiState.value.confirmandoTermino)
        assertEquals(emptyList<Long>(), api.sesionesTerminadas)
        assertEquals(FaseDelDojo.Ejercicio(primero), viewModel.uiState.value.fase)
    }

    @Test
    fun cancelarLaConfirmacionDejaLaSesionComoEstaba() {
        val viewModel = conSesionEnMarcha()
        viewModel.pedirTerminar()

        viewModel.cancelarTerminar()
        viewModel.confirmarTerminar()

        assertFalse(viewModel.uiState.value.confirmandoTermino)
        assertEquals(emptyList<Long>(), api.sesionesTerminadas)
        assertEquals(FaseDelDojo.Ejercicio(primero), viewModel.uiState.value.fase)
    }

    @Test
    fun confirmarSinHaberPedidoNoTermina() {
        val viewModel = conSesionEnMarcha()

        viewModel.confirmarTerminar()

        assertEquals(emptyList<Long>(), api.sesionesTerminadas)
    }

    // CA-55.4: al confirmar se termina la sesión, se muestra el resumen y ya no queda nada para retomar.
    @Test
    fun confirmarTerminaLaSesionMuestraElResumenYBorraLoGuardado() {
        api.alTerminar = { resumen }
        val viewModel = conSesionEnMarcha()
        viewModel.alResponder(12, acerto = true)
        viewModel.pedirTerminar()

        viewModel.confirmarTerminar()

        assertEquals(listOf(5L), api.sesionesTerminadas)
        assertEquals(FaseDelDojo.Terminada(resumen), viewModel.uiState.value.fase)
        assertNull(almacen.estado)
        assertNull(viewModel.uiState.value.sesionId)
        assertFalse(viewModel.uiState.value.confirmandoTermino)
    }

    // Al salir de la pantalla del resumen no se vuelve a guardar la sesión que ya se cerró.
    @Test
    fun salirDespuesDeTerminarNoVuelveAGuardarLaSesion() {
        val viewModel = conSesionEnMarcha()
        viewModel.pedirTerminar()
        viewModel.confirmarTerminar()

        viewModel.alSalir()

        assertNull(almacen.estado)
    }

    @Test
    fun terminarSePuedeTambienCuandoYaNoQuedanEjercicios() {
        api.ejercicios = mutableListOf(primero)
        val viewModel = crear()
        viewModel.elegirLenguaje("python")
        viewModel.alResponder(12, acerto = true)
        viewModel.siguiente()
        assertEquals(FaseDelDojo.SinMasEjercicios, viewModel.uiState.value.fase)

        viewModel.pedirTerminar()
        viewModel.confirmarTerminar()

        assertEquals(listOf(5L), api.sesionesTerminadas)
        assertEquals(FaseDelDojo.Terminada(ResumenDeSesion("facil", 0, 0, 0, emptyList(), null, null)), viewModel.uiState.value.fase)
    }

    @Test
    fun sinSesionNoSePuedePedirTerminar() {
        val viewModel = crear()

        viewModel.pedirTerminar()

        assertFalse(viewModel.uiState.value.confirmandoTermino)
    }

    @Test
    fun conLaOfertaDeContinuarNoSePuedePedirTerminar() {
        val viewModel = conSesionGuardada()

        viewModel.pedirTerminar()

        assertFalse(viewModel.uiState.value.confirmandoTermino)
    }

    // Sin señal, la sesión sigue donde estaba, con un aviso, y se puede volver a intentar.
    @Test
    fun terminarSinConexionDejaLaSesionYAvisa() {
        val viewModel = conSesionEnMarcha()
        api.alTerminar = { throw IOException("sin red") }
        viewModel.pedirTerminar()

        viewModel.confirmarTerminar()

        val estado = viewModel.uiState.value
        assertEquals(FaseDelDojo.Ejercicio(primero), estado.fase)
        assertEquals(R.string.dojo_error_conexion, estado.errorAlTerminar)
        assertEquals(EstadoDeSesion(5, "python", 12, 0, 0), almacen.estado)

        api.alTerminar = { resumen }
        viewModel.pedirTerminar()
        viewModel.confirmarTerminar()

        assertEquals(FaseDelDojo.Terminada(resumen), viewModel.uiState.value.fase)
        assertNull(viewModel.uiState.value.errorAlTerminar)
    }

    @Test
    fun terminarConUnErrorDelServidorDejaLaSesionYAvisa() {
        val viewModel = conSesionEnMarcha()
        api.alTerminar = { throw ErrorHttp(500) }
        viewModel.pedirTerminar()

        viewModel.confirmarTerminar()

        assertEquals(FaseDelDojo.Ejercicio(primero), viewModel.uiState.value.fase)
        assertEquals(R.string.dojo_error_inesperado, viewModel.uiState.value.errorAlTerminar)
    }

    // Si el servidor dice que ya estaba terminada, se trata como cerrada: se borra y se avisa.
    @Test
    fun terminarUnaSesionYaCerradaBorraLoGuardado() {
        val viewModel = conSesionEnMarcha()
        api.alTerminar = { throw ErrorHttp(409) }
        viewModel.pedirTerminar()

        viewModel.confirmarTerminar()

        assertEquals(FaseDelDojo.Error(R.string.dojo_error_sesion), viewModel.uiState.value.fase)
        assertNull(almacen.estado)
    }

    @Test
    fun terminarConLaSesionVencidaPideIniciarSesion() {
        val viewModel = conSesionEnMarcha()
        api.alTerminar = { throw ErrorHttp(401) }
        val auth401 = FakeAuthApi().apply { respuestaRenovar = { throw ErrorHttp(401) } }
        val vencido = DojoViewModel(
            DojoRepository(api, AuthRepository(auth401, MemoriaTokenStorage(refresh = "vencido"))),
            MemoriaSesion(),
        )
        api.ejercicios = mutableListOf(primero)
        vencido.elegirLenguaje("python")
        vencido.pedirTerminar()

        vencido.confirmarTerminar()

        assertEquals(FaseDelDojo.SesionVencida, vencido.uiState.value.fase)
        // La otra sesión, la del primer ViewModel, no se vio afectada.
        assertEquals(FaseDelDojo.Ejercicio(primero), viewModel.uiState.value.fase)
    }

    // ---------- HU55-T4: cerrar la app a media sesión y volver a abrirla ----------

    // Cerrar la app = el primer ViewModel desaparece tras avisar que sale; abrirla = un ViewModel nuevo con lo guardado.
    private fun cerrarYReabrir(viewModel: DojoViewModel): DojoViewModel {
        viewModel.alSalir()
        return crear()
    }

    @Test
    fun alReabrirLaAppSeOfreceLaMismaSesionConSusAciertos() {
        val viewModel = conSesionEnMarcha()
        viewModel.alResponder(12, acerto = true)
        viewModel.siguiente()
        viewModel.alResponder(13, acerto = false)

        val reabierto = cerrarYReabrir(viewModel)

        assertEquals(FaseDelDojo.OfreciendoReanudar(EstadoDeSesion(5, "python", 13, 1, 2)), reabierto.uiState.value.fase)
    }

    // Se cerró con un ejercicio sin responder: al continuar sale ese mismo ejercicio.
    @Test
    fun alReabrirYContinuarVuelveElEjercicioQueSeDejoSinResponder() {
        val viewModel = conSesionEnMarcha()
        viewModel.alResponder(12, acerto = true)
        viewModel.siguiente()
        val reabierto = cerrarYReabrir(viewModel)
        servidorDice(aciertos = 1, respondidos = 1, ejercicio = segundo)

        reabierto.reanudar()

        assertEquals(FaseDelDojo.Ejercicio(segundo), reabierto.uiState.value.fase)
        assertEquals(1, reabierto.uiState.value.aciertos)
        assertEquals(1, reabierto.uiState.value.respondidos)
    }

    // Se cerró justo después de responder: al continuar sigue con el siguiente ejercicio.
    @Test
    fun alReabrirDespuesDeResponderSigueConElSiguiente() {
        val viewModel = conSesionEnMarcha()
        viewModel.alResponder(12, acerto = true)
        val reabierto = cerrarYReabrir(viewModel)
        servidorDice(aciertos = 1, respondidos = 1, ejercicio = null)
        api.ejercicios = mutableListOf(segundo)

        reabierto.reanudar()

        assertEquals(FaseDelDojo.Ejercicio(segundo), reabierto.uiState.value.fase)
    }

    // Cerrar la app mientras se cargaba el ejercicio también deja la sesión guardada.
    @Test
    fun alCerrarLaAppMientrasCargabaElEjercicioLaSesionSigueGuardada() {
        api.alPedirSiguiente = { throw IOException("sin red") }
        val viewModel = crear()
        viewModel.elegirLenguaje("python")

        val reabierto = cerrarYReabrir(viewModel)

        assertEquals(FaseDelDojo.OfreciendoReanudar(EstadoDeSesion(5, "python", null, 0, 0)), reabierto.uiState.value.fase)
    }

    @Test
    fun alReabrirSinSenalLaOfertaSigueYAlVolverLaSenalSeContinua() {
        val viewModel = conSesionEnMarcha()
        val reabierto = cerrarYReabrir(viewModel)
        api.alConsultar = { throw IOException("sin red") }

        reabierto.reanudar()
        assertEquals(FaseDelDojo.OfreciendoReanudar(EstadoDeSesion(5, "python", 12, 0, 0), R.string.dojo_error_conexion), reabierto.uiState.value.fase)

        servidorDice(aciertos = 0, respondidos = 0, ejercicio = primero)
        reabierto.reanudar()

        assertEquals(FaseDelDojo.Ejercicio(primero), reabierto.uiState.value.fase)
    }

    // Una sesión terminada con «Terminar sesión» no se ofrece al reabrir.
    @Test
    fun alReabrirDespuesDeTerminarNoSeOfreceNada() {
        val viewModel = conSesionEnMarcha()
        viewModel.pedirTerminar()
        viewModel.confirmarTerminar()

        val reabierto = cerrarYReabrir(viewModel)

        assertEquals(FaseDelDojo.Eligiendo, reabierto.uiState.value.fase)
    }

    // Cerrar y reabrir dos veces seguidas sin decidir no pierde ni cambia la sesión.
    @Test
    fun cerrarYReabrirVariasVecesConservaLaMismaOferta() {
        val viewModel = conSesionEnMarcha()
        viewModel.alResponder(12, acerto = true)

        val segunda = cerrarYReabrir(cerrarYReabrir(viewModel))

        assertEquals(FaseDelDojo.OfreciendoReanudar(EstadoDeSesion(5, "python", 12, 1, 1)), segunda.uiState.value.fase)
    }
}
