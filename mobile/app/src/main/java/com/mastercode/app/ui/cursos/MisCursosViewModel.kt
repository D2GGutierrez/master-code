package com.mastercode.app.ui.cursos

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mastercode.app.R
import com.mastercode.app.data.cursos.CursoInscrito
import com.mastercode.app.data.cursos.MisCursosRepository
import com.mastercode.app.data.cursos.ResultadoMisCursos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Lo que muestra la pantalla: solo uno de estos casos a la vez.
sealed interface ContenidoMisCursos {
    data object Cargando : ContenidoMisCursos
    data class ConCursos(val cursos: List<CursoInscrito>) : ContenidoMisCursos
    data object Vacio : ContenidoMisCursos
    data class Error(@param:StringRes val mensaje: Int) : ContenidoMisCursos
    data object SesionVencida : ContenidoMisCursos
}

// cursoAbierto: el curso que el estudiante tocó, hasta que la pantalla lo atiende (como sesionIniciada en el login).
data class MisCursosUiState(
    val contenido: ContenidoMisCursos = ContenidoMisCursos.Cargando,
    val cursoAbierto: Long? = null,
)

class MisCursosViewModel(private val repository: MisCursosRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MisCursosUiState())
    val uiState: StateFlow<MisCursosUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    // También es la acción de «Reintentar».
    fun cargar() {
        _uiState.update { it.copy(contenido = ContenidoMisCursos.Cargando) }
        viewModelScope.launch {
            val contenido = when (val resultado = repository.misCursos()) {
                is ResultadoMisCursos.Exito ->
                    if (resultado.cursos.isEmpty()) ContenidoMisCursos.Vacio else ContenidoMisCursos.ConCursos(resultado.cursos)
                ResultadoMisCursos.SesionVencida -> ContenidoMisCursos.SesionVencida
                ResultadoMisCursos.SinConexion -> ContenidoMisCursos.Error(R.string.mis_cursos_error_conexion)
                ResultadoMisCursos.ErrorInesperado -> ContenidoMisCursos.Error(R.string.mis_cursos_error_inesperado)
            }
            _uiState.update { it.copy(contenido = contenido) }
        }
    }

    fun alTocarCurso(cursoId: Long) {
        _uiState.update { it.copy(cursoAbierto = cursoId) }
    }

    fun alCursoAbierto() {
        _uiState.update { it.copy(cursoAbierto = null) }
    }

    companion object {
        fun factory(repository: MisCursosRepository) = viewModelFactory {
            initializer { MisCursosViewModel(repository) }
        }
    }
}
