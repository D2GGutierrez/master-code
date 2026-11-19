package com.mastercode.app.ui.login

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mastercode.app.R
import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ResultadoLogin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// No guarda tokens: la sesión queda en el repositorio.
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    @param:StringRes val error: Int? = null,
    val sesionIniciada: Boolean = false,
) {
    val puedeEnviar: Boolean get() = !cargando && email.isNotBlank() && password.isNotEmpty()
}

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun alCambiarEmail(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun alCambiarPassword(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun alIniciarSesion() {
        val estado = _uiState.value
        // Evita el doble envío aunque el botón llegue a pulsarse dos veces.
        if (!estado.puedeEnviar) return
        _uiState.update { it.copy(cargando = true, error = null) }

        viewModelScope.launch { aplicar(repository.login(estado.email, estado.password)) }
    }

    // Google ya identificó a la persona en el teléfono; aquí se canjea su token por la sesión.
    fun alRecibirTokenDeGoogle(idToken: String) {
        if (_uiState.value.cargando) return
        _uiState.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch { aplicar(repository.loginConGoogle(idToken)) }
    }

    // El selector de cuentas de Google no se pudo mostrar (no hay cuentas, no hay Google Play...).
    fun alFallarGoogle(@StringRes error: Int) {
        _uiState.update { it.copy(error = error) }
    }

    private fun aplicar(resultado: ResultadoLogin) {
        _uiState.update {
            when (resultado) {
                // La contraseña se borra del estado en cuanto ya no hace falta.
                ResultadoLogin.Exito -> it.copy(cargando = false, password = "", sesionIniciada = true)
                ResultadoLogin.CredencialesIncorrectas -> it.copy(cargando = false, error = R.string.login_error_credenciales)
                ResultadoLogin.PendienteActivacion -> it.copy(cargando = false, error = R.string.login_error_pendiente)
                ResultadoLogin.GoogleRechazado -> it.copy(cargando = false, error = R.string.login_error_google_rechazado)
                ResultadoLogin.CuentaNoPermitida -> it.copy(cargando = false, error = R.string.login_error_google_no_permitida)
                ResultadoLogin.SinConexion -> it.copy(cargando = false, error = R.string.login_error_conexion)
                ResultadoLogin.ErrorInesperado -> it.copy(cargando = false, error = R.string.login_error_inesperado)
            }
        }
    }

    companion object {
        // Sin Hilt ni Koin: la fábrica recibe el repositorio ya creado.
        fun factory(repository: AuthRepository) = viewModelFactory {
            initializer { LoginViewModel(repository) }
        }
    }
}
