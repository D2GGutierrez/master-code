package com.mastercode.app.ui.perfil

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mastercode.app.R
import com.mastercode.app.data.cinturon.CinturonDelLenguaje
import com.mastercode.app.data.cinturon.CinturonesRepository
import com.mastercode.app.data.cinturon.ResultadoCinturones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Lo que muestra el perfil: solo uno de estos casos a la vez.
sealed interface ContenidoPerfil {
    data object Cargando : ContenidoPerfil

    // Todavía no acertó ningún ejercicio: no hay cinturón.
    data object SinCinturones : ContenidoPerfil
    data class ConCinturones(val cinturones: List<CinturonDelLenguaje>) : ContenidoPerfil
    data class Error(@param:StringRes val mensaje: Int) : ContenidoPerfil
    data object SesionVencida : ContenidoPerfil
}

data class PerfilUiState(val contenido: ContenidoPerfil = ContenidoPerfil.Cargando)

// HU51-T2 y HU51-T3 (CA-51.2 a CA-51.4): el perfil del estudiante con su cinturón por lenguaje, la barra de avance hacia
// el siguiente y, con el negro, un mensaje especial en lugar de la barra.
class PerfilViewModel(private val repository: CinturonesRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PerfilUiState())
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    // También es la acción de «Reintentar».
    fun cargar() {
        _uiState.update { it.copy(contenido = ContenidoPerfil.Cargando) }
        viewModelScope.launch {
            val contenido = when (val resultado = repository.cinturones()) {
                is ResultadoCinturones.Exito ->
                    if (resultado.cinturones.isEmpty()) ContenidoPerfil.SinCinturones else ContenidoPerfil.ConCinturones(resultado.cinturones)
                ResultadoCinturones.SesionVencida -> ContenidoPerfil.SesionVencida
                ResultadoCinturones.SinConexion -> ContenidoPerfil.Error(R.string.perfil_error_conexion)
                ResultadoCinturones.ErrorInesperado -> ContenidoPerfil.Error(R.string.perfil_error_inesperado)
            }
            _uiState.update { it.copy(contenido = contenido) }
        }
    }

    companion object {
        fun factory(repository: CinturonesRepository) = viewModelFactory {
            initializer { PerfilViewModel(repository) }
        }
    }
}
