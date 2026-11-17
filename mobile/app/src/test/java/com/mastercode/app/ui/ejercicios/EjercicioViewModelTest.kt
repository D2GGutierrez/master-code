package com.mastercode.app.ui.ejercicios

import com.mastercode.app.R
import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import com.mastercode.app.data.auth.Tokens
import com.mastercode.app.data.problemas.FakeProblemaApi
import com.mastercode.app.data.problemas.ProblemaDelEjercicio
import com.mastercode.app.data.problemas.ProblemaRepository
import com.mastercode.app.data.respuestas.EnviadorDeRespuestas
import com.mastercode.app.data.respuestas.MemoriaPendientes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class EjercicioViewModelTest {

    private val prediccion = ProblemaDelEjercicio(7, "Suma en un print", "¿Qué imprime?\n\nprint(2 + 3)", TIPO_PREDICCION_SALIDA)
    private val completar = ProblemaDelEjercicio(9, "Rellena el for", "for i in range(3):\n    print(___)", TIPO_COMPLETAR_ESPACIO)

    private val authApi = FakeAuthApi()
    private val problemaApi = FakeProblemaApi()

    // Hay un refresh guardado (sesión iniciada antes) pero ningún access en memoria, como al abrir la app.
    private val auth = AuthRepository(authApi, MemoriaTokenStorage(refresh = "refresh-guardado"))

    // viewModelScope usa Dispatchers.Main, que no existe en la JVM: se reemplaza por uno de prueba.
    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(problema: ProblemaDelEjercicio, tipoEsperado: String = problema.tipoEjercicio): EjercicioViewModel {
        val repository = ProblemaRepository(problemaApi, auth)
        return EjercicioViewModel(repository, EnviadorDeRespuestas(repository, MemoriaPendientes()), problema.id, tipoEsperado)
    }

    @Test
    fun pasaDeCargandoAlEjercicioYLoPideConSuId() {
        val respuesta = CompletableDeferred<ProblemaDelEjercicio>()
        problemaApi.respuesta = { _, _ -> respuesta.await() }

        val viewModel = crearViewModel(prediccion)
        assertEquals(ContenidoEjercicio.Cargando, viewModel.uiState.value.contenido)

        respuesta.complete(prediccion)
        assertEquals(ContenidoEjercicio.Listo(prediccion), viewModel.uiState.value.contenido)
        assertEquals(listOf(7L), problemaApi.idsPedidos)
    }

    @Test
    fun elMismoViewModelSirveParaCompletarElEspacio() {
        problemaApi.respuesta = { _, _ -> completar }

        val viewModel = crearViewModel(completar)

        assertEquals(ContenidoEjercicio.Listo(completar), viewModel.uiState.value.contenido)
    }

    @Test
    fun loEscritoSeConservaYEmpiezaVacio() {
        problemaApi.respuesta = { _, _ -> prediccion }
        val viewModel = crearViewModel(prediccion)
        assertEquals("", viewModel.uiState.value.respuesta)

        viewModel.alCambiarRespuesta("5")
        viewModel.alCambiarRespuesta("5 ")

        assertEquals("5 ", viewModel.uiState.value.respuesta)
    }

    // Reintentar no borra lo que el estudiante ya escribió.
    @Test
    fun reintentarTrasUnErrorCargaElEjercicioYNoBorraLoEscrito() {
        problemaApi.respuesta = { _, _ -> throw IOException("sin red") }
        val viewModel = crearViewModel(prediccion)
        viewModel.alCambiarRespuesta("5")
        assertEquals(ContenidoEjercicio.Error(R.string.ejercicio_error_conexion), viewModel.uiState.value.contenido)

        problemaApi.respuesta = { _, _ -> prediccion }
        viewModel.cargar()

        assertEquals(ContenidoEjercicio.Listo(prediccion), viewModel.uiState.value.contenido)
        assertEquals("5", viewModel.uiState.value.respuesta)
    }

    @Test
    fun unEjercicioDeOtroTipoNoSeMuestraEnLaPantallaEquivocada() {
        problemaApi.respuesta = { _, _ -> completar }

        val viewModel = crearViewModel(completar, tipoEsperado = TIPO_PREDICCION_SALIDA)

        assertEquals(ContenidoEjercicio.Error(R.string.ejercicio_error_tipo), viewModel.uiState.value.contenido)
    }

    @Test
    fun un404MuestraQueNoSeEncontro() {
        problemaApi.respuesta = { _, _ -> throw ErrorHttp(404) }

        val viewModel = crearViewModel(prediccion)

        assertEquals(ContenidoEjercicio.Error(R.string.ejercicio_error_no_encontrado), viewModel.uiState.value.contenido)
    }

    @Test
    fun unErrorDelServidorMuestraElErrorInesperado() {
        problemaApi.respuesta = { _, _ -> throw ErrorHttp(500) }

        val viewModel = crearViewModel(prediccion)

        assertEquals(ContenidoEjercicio.Error(R.string.ejercicio_error_inesperado), viewModel.uiState.value.contenido)
    }

    @Test
    fun un401QueNoSePuedeRenovarMuestraSesionVencida() {
        problemaApi.respuesta = { _, _ -> throw ErrorHttp(401) }
        authApi.respuestaRenovar = { throw ErrorHttp(401) }

        val viewModel = crearViewModel(prediccion)

        assertEquals(ContenidoEjercicio.SesionVencida, viewModel.uiState.value.contenido)
        // AuthRepository ya borró el refresh: al volver a abrir la app se pedirá login.
        assertEquals(false, auth.haySesionGuardada())
    }

    @Test
    fun un401ConAccessVencidoSeRenuevaYSeReintentaConElNuevo() {
        problemaApi.respuesta = { access, _ -> if (access == "access-viejo") throw ErrorHttp(401) else prediccion }
        authApi.respuestaLogin = { Tokens(access = "access-viejo", refresh = "refresh-1") }
        runBlocking { auth.login("ana@ejemplo.edu.pe", "clave-inventada") }

        val viewModel = crearViewModel(prediccion)

        assertEquals(ContenidoEjercicio.Listo(prediccion), viewModel.uiState.value.contenido)
        // FakeAuthApi renueva con "access-2": se usó el viejo, falló, y se reintentó con el nuevo.
        assertEquals(listOf("access-viejo", "access-2"), problemaApi.accessRecibidos)
    }
}
