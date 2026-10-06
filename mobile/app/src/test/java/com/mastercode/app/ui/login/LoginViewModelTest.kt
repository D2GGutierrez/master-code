package com.mastercode.app.ui.login

import com.mastercode.app.R
import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import com.mastercode.app.data.auth.Tokens
import kotlinx.coroutines.CompletableDeferred
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

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val api = FakeAuthApi()
    private val storage = MemoriaTokenStorage()
    private val viewModel by lazy { LoginViewModel(AuthRepository(api, storage)) }

    // viewModelScope usa Dispatchers.Main, que no existe en la JVM: se reemplaza por uno de prueba.
    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun escribirYEnviar() {
        viewModel.alCambiarEmail("ana@tecsup.edu.pe")
        viewModel.alCambiarPassword("clave-segura")
        viewModel.alIniciarSesion()
    }

    @Test
    fun loginCorrectoIniciaSesionYGuardaElRefresh() {
        escribirYEnviar()

        val estado = viewModel.uiState.value
        assertTrue(estado.sesionIniciada)
        assertEquals("", estado.password)
        assertNull(estado.error)
        assertEquals("refresh-1", storage.refresh)
    }

    @Test
    fun credencialesIncorrectasMuestranErrorYNoGuardanNada() {
        api.respuestaLogin = { throw ErrorHttp(401) }

        escribirYEnviar()

        val estado = viewModel.uiState.value
        assertEquals(R.string.login_error_credenciales, estado.error)
        assertFalse(estado.sesionIniciada)
        assertFalse(estado.cargando)
        assertNull(storage.refresh)
    }

    @Test
    fun cuentaInactivaMuestraPendienteDeActivacion() {
        api.respuestaLogin = { throw ErrorHttp(403) }

        escribirYEnviar()

        assertEquals(R.string.login_error_pendiente, viewModel.uiState.value.error)
    }

    @Test
    fun sinConexionMuestraErrorDeConexion() {
        api.respuestaLogin = { throw IOException() }

        escribirYEnviar()

        assertEquals(R.string.login_error_conexion, viewModel.uiState.value.error)
    }

    @Test
    fun mientrasCargaNoSePuedeEnviarOtraVez() {
        val respuesta = CompletableDeferred<Tokens>()
        var llamadas = 0
        api.respuestaLogin = { llamadas++; respuesta.await() }

        escribirYEnviar()
        viewModel.alIniciarSesion()

        assertTrue(viewModel.uiState.value.cargando)
        assertFalse(viewModel.uiState.value.puedeEnviar)
        assertEquals(1, llamadas)

        respuesta.complete(Tokens(access = "a", refresh = "r"))
        assertFalse(viewModel.uiState.value.cargando)
    }
}
