package com.mastercode.app.ui.cursos

import com.mastercode.app.R
import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import com.mastercode.app.data.auth.Tokens
import com.mastercode.app.data.cursos.CursoInscrito
import com.mastercode.app.data.cursos.FakeMisCursosApi
import com.mastercode.app.data.cursos.MisCursosRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MisCursosViewModelTest {

    private val cursos = listOf(
        CursoInscrito(11, "Java Básico", "java", LocalDate.of(2026, 10, 27)),
        CursoInscrito(10, "Algoritmos I", "python", LocalDate.of(2026, 10, 20)),
    )

    private val authApi = FakeAuthApi()
    private val cursosApi = FakeMisCursosApi()

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

    private fun crearViewModel() = MisCursosViewModel(MisCursosRepository(cursosApi, auth))

    @Test
    fun pasaDeCargandoALaListaDeCursos() {
        val respuesta = CompletableDeferred<List<CursoInscrito>>()
        cursosApi.respuesta = { respuesta.await() }

        val viewModel = crearViewModel()
        assertEquals(ContenidoMisCursos.Cargando, viewModel.uiState.value.contenido)

        respuesta.complete(cursos)
        assertEquals(ContenidoMisCursos.ConCursos(cursos), viewModel.uiState.value.contenido)
    }

    @Test
    fun unaListaVaciaMuestraElEstadoVacio() {
        cursosApi.respuesta = { emptyList() }

        val viewModel = crearViewModel()

        assertEquals(ContenidoMisCursos.Vacio, viewModel.uiState.value.contenido)
    }

    @Test
    fun sinConexionMuestraErrorYReintentarCargaLosCursos() {
        cursosApi.respuesta = { throw IOException("sin red") }
        val viewModel = crearViewModel()
        assertEquals(ContenidoMisCursos.Error(R.string.mis_cursos_error_conexion), viewModel.uiState.value.contenido)

        cursosApi.respuesta = { cursos }
        viewModel.cargar()

        assertEquals(ContenidoMisCursos.ConCursos(cursos), viewModel.uiState.value.contenido)
    }

    @Test
    fun un401QueNoSePuedeRenovarMuestraSesionVencida() {
        cursosApi.respuesta = { throw ErrorHttp(401) }
        authApi.respuestaRenovar = { throw ErrorHttp(401) }

        val viewModel = crearViewModel()

        assertEquals(ContenidoMisCursos.SesionVencida, viewModel.uiState.value.contenido)
        // AuthRepository ya borró el refresh: al volver a abrir la app se pedirá login.
        assertEquals(false, auth.haySesionGuardada())
    }

    @Test
    fun un401ConAccessVencidoSeRenuevaYSeReintentaConElNuevo() {
        cursosApi.respuesta = { access -> if (access == "access-viejo") throw ErrorHttp(401) else cursos }
        authApi.respuestaLogin = { Tokens(access = "access-viejo", refresh = "refresh-1") }
        runBlocking { auth.login("ana@ejemplo.edu.pe", "clave-inventada") }

        val viewModel = crearViewModel()

        assertEquals(ContenidoMisCursos.ConCursos(cursos), viewModel.uiState.value.contenido)
        // FakeAuthApi renueva con "access-2": se usó el viejo, falló, y se reintentó con el nuevo.
        assertEquals(listOf("access-viejo", "access-2"), cursosApi.accessRecibidos)
    }

    @Test
    fun tocarUnaTarjetaEmiteElIdDelCursoHastaQueLaPantallaLoAtiende() {
        cursosApi.respuesta = { cursos }
        val viewModel = crearViewModel()

        viewModel.alTocarCurso(10)
        assertEquals(10L, viewModel.uiState.value.cursoAbierto)

        viewModel.alCursoAbierto()
        assertNull(viewModel.uiState.value.cursoAbierto)
    }
}
