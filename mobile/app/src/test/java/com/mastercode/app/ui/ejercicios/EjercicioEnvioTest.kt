package com.mastercode.app.ui.ejercicios

import com.mastercode.app.R
import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import com.mastercode.app.data.problemas.FakeProblemaApi
import com.mastercode.app.data.problemas.OpcionDelEjercicio
import com.mastercode.app.data.problemas.ProblemaDelEjercicio
import com.mastercode.app.data.problemas.ProblemaRepository
import com.mastercode.app.data.problemas.RespuestaDelEstudiante
import com.mastercode.app.data.problemas.ResultadoDeRespuesta
import com.mastercode.app.data.respuestas.EnviadorDeRespuestas
import com.mastercode.app.data.respuestas.MemoriaPendientes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

// HU45-T3, HU46-T3, HU48-T2, HU48-T3 en la pantalla: enviar, mostrar al instante si acertó y cuál era la correcta, los
// intentos que quedan y qué pasa sin conexión.
@OptIn(ExperimentalCoroutinesApi::class)
class EjercicioEnvioTest {

    private val prediccion = ProblemaDelEjercicio(7, "Suma en un print", "¿Qué imprime?\n\nprint(2 + 3)", TIPO_PREDICCION_SALIDA, intentosRestantes = 3)
    private val multiple = ProblemaDelEjercicio(
        12, "Elige", "¿Cuál imprime 5?", TIPO_OPCION_MULTIPLE,
        listOf(OpcionDelEjercicio(31, "print(5)"), OpcionDelEjercicio(32, "print(4)")), 3,
    )

    private val problemaApi = FakeProblemaApi()
    private val almacen = MemoriaPendientes()
    private val auth = AuthRepository(FakeAuthApi(), MemoriaTokenStorage(refresh = "refresh-guardado"))
    private val repository = ProblemaRepository(problemaApi, auth)
    private val enviador = EnviadorDeRespuestas(repository, almacen)
    private var numeroDeIntento = 0

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    // El ejercicio llega ya cargado, como lo entrega el Dojo.
    private fun crear(problema: ProblemaDelEjercicio, sesionId: Long? = null) = EjercicioViewModel(
        repository, enviador, problema.id, problema.tipoEjercicio, sesionId, precargado = problema,
        nuevoIntentoId = { "intento-${++numeroDeIntento}" },
    )

    private fun responder(resultado: ResultadoDeRespuesta) {
        problemaApi.alResponder = { _, _ -> resultado }
    }

    @Test
    fun elEjercicioPrecargadoNoSePideAlServidor() {
        val viewModel = crear(prediccion)

        assertEquals(ContenidoEjercicio.Listo(prediccion), viewModel.uiState.value.contenido)
        assertEquals(emptyList<Long>(), problemaApi.idsPedidos)
        assertEquals(3, viewModel.uiState.value.intentosRestantes)
    }

    @Test
    fun noSePuedeEnviarSinContestar() {
        val viewModel = crear(prediccion)
        assertFalse(viewModel.uiState.value.puedeEnviar)

        viewModel.enviar()
        assertEquals(emptyList<RespuestaDelEstudiante>(), problemaApi.respuestasRecibidas)

        viewModel.alCambiarRespuesta("   ")
        assertFalse(viewModel.uiState.value.puedeEnviar)
        viewModel.alCambiarRespuesta("5")
        assertTrue(viewModel.uiState.value.puedeEnviar)
    }

    // CA-48.2: acertar se muestra de inmediato, sin respuesta correcta que mostrar.
    @Test
    fun alAcertarMuestraElResultadoYLosIntentosQueQuedan() {
        responder(ResultadoDeRespuesta(acerto = true, respuestaCorrecta = null, intentosRestantes = 2))
        val viewModel = crear(prediccion)
        viewModel.alCambiarRespuesta("5")

        viewModel.enviar()

        assertEquals(EstadoDeEnvio.Resultado(acerto = true, respuestaCorrecta = null), viewModel.uiState.value.envio)
        assertEquals(2, viewModel.uiState.value.intentosRestantes)
        // Lo contestado viaja como texto, con un identificador de intento y sin sesión.
        assertEquals(listOf(RespuestaDelEstudiante(7, "5", null, null, "intento-1")), problemaApi.respuestasRecibidas)
    }

    // CA-48.3: al fallar se muestra cuál era la correcta.
    @Test
    fun alFallarMuestraLaRespuestaCorrecta() {
        responder(ResultadoDeRespuesta(acerto = false, respuestaCorrecta = "5", intentosRestantes = 2))
        val viewModel = crear(prediccion)
        viewModel.alCambiarRespuesta("4")

        viewModel.enviar()

        assertEquals(EstadoDeEnvio.Resultado(acerto = false, respuestaCorrecta = "5"), viewModel.uiState.value.envio)
    }

    @Test
    fun conOpcionMultipleSeEnvianLaAlternativaElegidaYNoTexto() {
        responder(ResultadoDeRespuesta(acerto = true, respuestaCorrecta = null, intentosRestantes = 2))
        val viewModel = crear(multiple)
        assertFalse(viewModel.uiState.value.puedeEnviar)

        viewModel.alElegirOpcion(32)
        viewModel.alElegirOpcion(31)
        assertTrue(viewModel.uiState.value.puedeEnviar)
        viewModel.enviar()

        assertEquals(listOf(RespuestaDelEstudiante(12, null, 31, null, "intento-1")), problemaApi.respuestasRecibidas)
    }

    @Test
    fun trasLaCorreccionYaNoSeCambiaLoContestado() {
        responder(ResultadoDeRespuesta(acerto = false, respuestaCorrecta = "5", intentosRestantes = 2))
        val viewModel = crear(prediccion)
        viewModel.alCambiarRespuesta("4")
        viewModel.enviar()

        viewModel.alCambiarRespuesta("5")
        viewModel.enviar()

        assertEquals("4", viewModel.uiState.value.respuesta)
        assertEquals(1, problemaApi.respuestasRecibidas.size)
    }

    // CA-45.5: fuera del Dojo, tras fallar con intentos por gastar se puede intentar otra vez, con otro intento_id.
    @Test
    fun trasFallarSePuedeIntentarDeNuevoMientrasQuedenIntentos() {
        responder(ResultadoDeRespuesta(acerto = false, respuestaCorrecta = "5", intentosRestantes = 2))
        val viewModel = crear(prediccion)
        viewModel.alCambiarRespuesta("4")
        viewModel.enviar()
        assertTrue(viewModel.uiState.value.puedeIntentarDeNuevo)

        viewModel.intentarDeNuevo()
        assertEquals("", viewModel.uiState.value.respuesta)
        assertEquals(EstadoDeEnvio.SinEnviar, viewModel.uiState.value.envio)
        assertEquals(2, viewModel.uiState.value.intentosRestantes)

        viewModel.alCambiarRespuesta("5")
        viewModel.enviar()
        assertEquals(listOf("intento-1", "intento-2"), problemaApi.respuestasRecibidas.map { it.intentoId })
    }

    @Test
    fun sinIntentosPorGastarOTrasAcertarNoSePuedeIntentarDeNuevo() {
        responder(ResultadoDeRespuesta(acerto = false, respuestaCorrecta = "5", intentosRestantes = 0))
        val viewModel = crear(prediccion)
        viewModel.alCambiarRespuesta("4")
        viewModel.enviar()

        assertFalse(viewModel.uiState.value.puedeIntentarDeNuevo)
        assertEquals(0, viewModel.uiState.value.intentosRestantes)

        responder(ResultadoDeRespuesta(acerto = true, respuestaCorrecta = null, intentosRestantes = 2))
        val otro = crear(prediccion.copy(id = 8))
        otro.alCambiarRespuesta("5")
        otro.enviar()
        assertFalse(otro.uiState.value.puedeIntentarDeNuevo)
    }

    // Dentro del Dojo cada ejercicio se responde una vez y la respuesta lleva el id de la sesión.
    @Test
    fun dentroDeUnaSesionDelDojoEnviaElSesionIdYNoOfreceIntentarDeNuevo() {
        responder(ResultadoDeRespuesta(acerto = false, respuestaCorrecta = "5", intentosRestantes = 2))
        val viewModel = crear(prediccion, sesionId = 41)
        viewModel.alCambiarRespuesta("4")

        viewModel.enviar()

        assertEquals(41L, problemaApi.respuestasRecibidas.single().sesionId)
        assertFalse(viewModel.uiState.value.puedeIntentarDeNuevo)
    }

    // CA-48.4: sin conexión la respuesta queda guardada en el teléfono.
    @Test
    fun sinConexionLaRespuestaQuedaGuardadaYLaPantallaLoDice() {
        problemaApi.alResponder = { _, _ -> throw IOException("sin red") }
        val viewModel = crear(prediccion)
        viewModel.alCambiarRespuesta("5")

        viewModel.enviar()

        assertEquals(EstadoDeEnvio.Pendiente, viewModel.uiState.value.envio)
        assertEquals(listOf(RespuestaDelEstudiante(7, "5", null, null, "intento-1")), almacen.guardadas)
        assertFalse(viewModel.uiState.value.editable)
    }

    // Al volver la señal la respuesta se envía sola y la pantalla pasa a mostrar el resultado (CA-48.4).
    @Test
    fun alVolverLaSenalLaPantallaMuestraElResultadoDeLaRespuestaGuardada() = runBlocking {
        problemaApi.alResponder = { _, _ -> throw IOException("sin red") }
        val viewModel = crear(prediccion)
        viewModel.alCambiarRespuesta("4")
        viewModel.enviar()

        responder(ResultadoDeRespuesta(acerto = false, respuestaCorrecta = "5", intentosRestantes = 2))
        enviador.reenviarPendientes()

        assertEquals(EstadoDeEnvio.Resultado(acerto = false, respuestaCorrecta = "5"), viewModel.uiState.value.envio)
        assertEquals(2, viewModel.uiState.value.intentosRestantes)
        assertEquals(emptyList<RespuestaDelEstudiante>(), almacen.guardadas)
        // Se reenvió el mismo intento, no uno nuevo (CA-48.5).
        assertEquals(listOf("intento-1", "intento-1"), problemaApi.respuestasRecibidas.map { it.intentoId })
    }

    // Se sale del ejercicio sin señal y se vuelve a entrar: la respuesta guardada se retoma.
    @Test
    fun alVolverAEntrarSeRetomaLaRespuestaGuardada() {
        problemaApi.alResponder = { _, _ -> throw IOException("sin red") }
        crear(prediccion).apply {
            alCambiarRespuesta("5")
            enviar()
        }

        val nuevo = crear(prediccion)

        assertEquals(EstadoDeEnvio.Pendiente, nuevo.uiState.value.envio)
        assertEquals("5", nuevo.uiState.value.respuesta)
    }

    @Test
    fun siElServidorRechazaPorFaltaDeIntentosMuestraElMotivoYSePuedeCorregir() {
        problemaApi.alResponder = { _, _ -> throw ErrorHttp(409) }
        val viewModel = crear(prediccion)
        viewModel.alCambiarRespuesta("5")

        viewModel.enviar()

        assertEquals(EstadoDeEnvio.Rechazado(R.string.ejercicio_rechazado_conflicto), viewModel.uiState.value.envio)
    }

    @Test
    fun unaRespuestaInvalidaOUnErrorDelServidorSePuedenVolverAEnviarConElMismoIntento() {
        problemaApi.alResponder = { _, _ -> throw ErrorHttp(500) }
        val viewModel = crear(prediccion)
        viewModel.alCambiarRespuesta("5")
        viewModel.enviar()
        assertEquals(EstadoDeEnvio.Rechazado(R.string.ejercicio_rechazado_inesperado), viewModel.uiState.value.envio)
        assertTrue(viewModel.uiState.value.puedeEnviar)

        responder(ResultadoDeRespuesta(acerto = true, respuestaCorrecta = null, intentosRestantes = 2))
        viewModel.enviar()

        assertEquals(EstadoDeEnvio.Resultado(acerto = true, respuestaCorrecta = null), viewModel.uiState.value.envio)
        assertEquals(listOf("intento-1", "intento-1"), problemaApi.respuestasRecibidas.map { it.intentoId })
    }

    @Test
    fun unaSesionVencidaAlEnviarPideIniciarSesion() {
        problemaApi.alResponder = { _, _ -> throw ErrorHttp(401) }
        val auth401 = FakeAuthApi().apply { respuestaRenovar = { throw ErrorHttp(401) } }
        val repositorio = ProblemaRepository(problemaApi, AuthRepository(auth401, MemoriaTokenStorage(refresh = "vencido")))
        val viewModel = EjercicioViewModel(
            repositorio, EnviadorDeRespuestas(repositorio, almacen), 7, TIPO_PREDICCION_SALIDA, precargado = prediccion,
        )
        viewModel.alCambiarRespuesta("5")

        viewModel.enviar()

        assertEquals(ContenidoEjercicio.SesionVencida, viewModel.uiState.value.contenido)
    }

    // HU46-T3, CA-46.5: los botones de las alternativas son grandes.
    @Test
    fun losBotonesDeAlternativaMidenAlMenos48dp() {
        assertTrue(ALTURA_MINIMA_OPCION_DP >= 48)
    }
}
