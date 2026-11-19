package com.mastercode.app.ui.duelos

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mastercode.app.R
import com.mastercode.app.data.cursos.MisCursosRepository
import com.mastercode.app.data.duelos.Companero
import com.mastercode.app.data.duelos.DuelosRepository
import com.mastercode.app.data.duelos.ResultadoDuelo
import com.mastercode.app.data.duelos.RetoCreado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Los compañeros del curso elegido.
sealed interface ContenidoDeCompaneros {
    // Todavía no se eligió curso.
    data object SinCurso : ContenidoDeCompaneros
    data object Cargando : ContenidoDeCompaneros
    data class Lista(val companeros: List<Companero>) : ContenidoDeCompaneros
    // El curso no tiene a nadie más inscrito.
    data object Vacio : ContenidoDeCompaneros
    data class Error(@param:StringRes val mensaje: Int) : ContenidoDeCompaneros
}

// retando: el compañero al que se está retando (se bloquea el resto mientras tanto). retoEnviado: el reto ya creado; la
// pantalla abre la partida y avisa con alAbrirseLaPartida. error: por qué no se pudo retar.
data class RetarUiState(
    val cursos: ContenidoDeCursos = ContenidoDeCursos.Cargando,
    val cursoElegido: Long? = null,
    val companeros: ContenidoDeCompaneros = ContenidoDeCompaneros.SinCurso,
    val retando: Long? = null,
    @param:StringRes val error: Int? = null,
    val retoEnviado: RetoCreado? = null,
    val sesionVencida: Boolean = false,
)

// HU57-T3: la pantalla para retar. Se elige el curso (si el estudiante solo tiene uno, ya viene elegido), aparecen los
// compañeros inscritos y al tocar uno se crea el reto (HU57-T2).
class RetarViewModel(
    private val misCursos: MisCursosRepository,
    private val duelos: DuelosRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RetarUiState())
    val uiState: StateFlow<RetarUiState> = _uiState.asStateFlow()

    init {
        cargarCursos()
    }

    // También es la acción de «Reintentar».
    fun cargarCursos() {
        _uiState.update { it.copy(cursos = ContenidoDeCursos.Cargando) }
        viewModelScope.launch {
            val cursos = misCursos.cursosParaDuelos()
            _uiState.update { it.copy(cursos = cursos, sesionVencida = cursos == ContenidoDeCursos.SesionVencida) }
            val unico = (cursos as? ContenidoDeCursos.Lista)?.cursos?.singleOrNull()
            if (unico != null) elegirCurso(unico.cursoId)
        }
    }

    fun elegirCurso(cursoId: Long) {
        _uiState.update { it.copy(cursoElegido = cursoId, companeros = ContenidoDeCompaneros.Cargando, error = null) }
        viewModelScope.launch {
            val contenido = when (val resultado = duelos.companeros(cursoId)) {
                is ResultadoDuelo.Exito ->
                    if (resultado.valor.isEmpty()) ContenidoDeCompaneros.Vacio else ContenidoDeCompaneros.Lista(resultado.valor)
                is ResultadoDuelo.Rechazado -> ContenidoDeCompaneros.Error(R.string.retar_error_no_inscrito)
                ResultadoDuelo.SinConexion -> ContenidoDeCompaneros.Error(R.string.duelos_error_conexion)
                ResultadoDuelo.SesionVencida -> {
                    _uiState.update { it.copy(sesionVencida = true) }
                    ContenidoDeCompaneros.Error(R.string.duelos_error_inesperado)
                }
                ResultadoDuelo.ErrorInesperado -> ContenidoDeCompaneros.Error(R.string.duelos_error_inesperado)
            }
            // Si mientras tanto eligió otro curso, esta respuesta ya no vale.
            _uiState.update { if (it.cursoElegido == cursoId) it.copy(companeros = contenido) else it }
        }
    }

    fun retar(companero: Companero) {
        val estado = _uiState.value
        val cursoId = estado.cursoElegido ?: return
        if (estado.retando != null) return
        _uiState.update { it.copy(retando = companero.id, error = null) }
        viewModelScope.launch {
            val resultado = duelos.retar(cursoId, companero.id)
            _uiState.update {
                when (resultado) {
                    is ResultadoDuelo.Exito -> it.copy(retando = null, retoEnviado = resultado.valor)
                    is ResultadoDuelo.Rechazado -> it.copy(retando = null, error = errorDelReto(resultado.codigo))
                    ResultadoDuelo.SinConexion -> it.copy(retando = null, error = R.string.duelos_error_conexion)
                    ResultadoDuelo.SesionVencida -> it.copy(retando = null, sesionVencida = true)
                    ResultadoDuelo.ErrorInesperado -> it.copy(retando = null, error = R.string.duelos_error_inesperado)
                }
            }
        }
    }

    // La pantalla ya abrió la partida del reto enviado.
    fun alAbrirseLaPartida() {
        _uiState.update { it.copy(retoEnviado = null) }
    }

    @StringRes
    private fun errorDelReto(codigo: Int): Int = when (codigo) {
        400 -> R.string.retar_error_rival
        403 -> R.string.retar_error_no_inscrito
        409 -> R.string.retar_error_ya_hay_reto
        else -> R.string.retar_error_no_disponible
    }

    companion object {
        fun factory(misCursos: MisCursosRepository, duelos: DuelosRepository) = viewModelFactory {
            initializer { RetarViewModel(misCursos, duelos) }
        }
    }
}
