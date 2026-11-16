package com.mastercode.app.ui.ejercicios

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mastercode.app.R
import com.mastercode.app.data.problemas.ProblemaDelEjercicio
import com.mastercode.app.data.problemas.ProblemaRepository
import com.mastercode.app.data.problemas.ResultadoProblema
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val TIPO_PREDICCION_SALIDA = "prediccion_salida"
const val TIPO_COMPLETAR_ESPACIO = "completar_espacio"

// Lo que muestra la pantalla: solo uno de estos casos a la vez.
sealed interface ContenidoEjercicio {
    data object Cargando : ContenidoEjercicio
    data class Listo(val problema: ProblemaDelEjercicio) : ContenidoEjercicio
    data class Error(@param:StringRes val mensaje: Int) : ContenidoEjercicio
    data object SesionVencida : ContenidoEjercicio
}

// respuesta es lo que el estudiante lleva escrito; enviarla es HU45-T3 y HU48-T2.
data class EjercicioUiState(
    val contenido: ContenidoEjercicio = ContenidoEjercicio.Cargando,
    val respuesta: String = "",
)

// Sirve a las dos pantallas («predecir la salida» y «completar el espacio»): cargan y guardan lo escrito igual, y solo se
// dibujan distinto. tipoEsperado evita mostrar un ejercicio de otro tipo en la pantalla equivocada.
class EjercicioViewModel(
    private val repository: ProblemaRepository,
    private val problemaId: Long,
    private val tipoEsperado: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EjercicioUiState())
    val uiState: StateFlow<EjercicioUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    // También es la acción de «Reintentar».
    fun cargar() {
        _uiState.update { it.copy(contenido = ContenidoEjercicio.Cargando) }
        viewModelScope.launch {
            val contenido = when (val resultado = repository.problema(problemaId)) {
                is ResultadoProblema.Exito ->
                    if (resultado.problema.tipoEjercicio == tipoEsperado) {
                        ContenidoEjercicio.Listo(resultado.problema)
                    } else {
                        ContenidoEjercicio.Error(R.string.ejercicio_error_tipo)
                    }
                ResultadoProblema.NoEncontrado -> ContenidoEjercicio.Error(R.string.ejercicio_error_no_encontrado)
                ResultadoProblema.SesionVencida -> ContenidoEjercicio.SesionVencida
                ResultadoProblema.SinConexion -> ContenidoEjercicio.Error(R.string.ejercicio_error_conexion)
                ResultadoProblema.ErrorInesperado -> ContenidoEjercicio.Error(R.string.ejercicio_error_inesperado)
            }
            _uiState.update { it.copy(contenido = contenido) }
        }
    }

    fun alCambiarRespuesta(texto: String) {
        _uiState.update { it.copy(respuesta = texto) }
    }

    companion object {
        fun factory(repository: ProblemaRepository, problemaId: Long, tipoEsperado: String) = viewModelFactory {
            initializer { EjercicioViewModel(repository, problemaId, tipoEsperado) }
        }
    }
}
