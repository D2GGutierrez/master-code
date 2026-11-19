package com.mastercode.app.ui.duelos

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mastercode.app.R
import com.mastercode.app.data.cursos.MisCursosRepository
import com.mastercode.app.data.duelos.DuelosRepository
import com.mastercode.app.data.duelos.PuestoDelRanking
import com.mastercode.app.data.duelos.ResultadoDuelo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// puestos son las páginas ya traídas, una tras otra. yo es el puesto del estudiante esté o no en esas páginas (con ceros si todavía no jugó). cargando: la primera página del curso elegido.
data class RankingUiState(
    val cursos: ContenidoDeCursos = ContenidoDeCursos.Cargando,
    val cursoElegido: Long? = null,
    val puestos: List<PuestoDelRanking> = emptyList(),
    val yo: PuestoDelRanking? = null,
    val pagina: Int = 0,
    val total: Int = 0,
    val cargando: Boolean = false,
    val cargandoMas: Boolean = false,
    @param:StringRes val error: Int? = null,
    val sesionVencida: Boolean = false,
) {
    val hayMas: Boolean get() = puestos.size < total

    // Mi puesto se muestra aparte, fijo abajo, cuando no aparece entre las filas ya cargadas (HU60-T3).
    val miPuestoFueraDeLaLista: PuestoDelRanking? get() = yo?.takeIf { mio -> puestos.none { it.soyYo } }

    // El estudiante todavía no jugó en este curso: user-api lo incluye al final del ranking, con ceros (HU60-T4).
    val sinPartidas: Boolean get() = cursoElegido != null && !cargando && error == null && (yo == null || yo.jugadas == 0)
}

// HU60-T3: el ranking del curso elegido, de a 20, con el puesto del estudiante siempre visible (resaltado en la lista o fijo
// aparte si todavía no se cargó su página).
class RankingViewModel(
    private val misCursos: MisCursosRepository,
    private val duelos: DuelosRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RankingUiState())
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

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
        _uiState.update {
            it.copy(cursoElegido = cursoId, puestos = emptyList(), yo = null, pagina = 0, total = 0, cargando = true, cargandoMas = false, error = null)
        }
        pedir(cursoId, 1)
    }

    // «Reintentar» el curso elegido.
    fun recargar() {
        _uiState.value.cursoElegido?.let { elegirCurso(it) }
    }

    fun cargarMas() {
        val estado = _uiState.value
        val cursoId = estado.cursoElegido ?: return
        if (estado.cargando || estado.cargandoMas || !estado.hayMas) return
        _uiState.update { it.copy(cargandoMas = true, error = null) }
        pedir(cursoId, estado.pagina + 1)
    }

    private fun pedir(cursoId: Long, pagina: Int) {
        viewModelScope.launch {
            val resultado = duelos.ranking(cursoId, pagina, TAMANO_DE_PAGINA)
            _uiState.update { estado ->
                // Si mientras tanto eligió otro curso, esta respuesta ya no vale.
                if (estado.cursoElegido != cursoId) return@update estado
                when (resultado) {
                    is ResultadoDuelo.Exito -> estado.copy(
                        puestos = if (pagina == 1) resultado.valor.items else estado.puestos + resultado.valor.items,
                        yo = resultado.valor.yo,
                        pagina = pagina,
                        total = resultado.valor.total,
                        cargando = false,
                        cargandoMas = false,
                        error = null,
                    )
                    // 403: el curso ya no es del estudiante (HU60-T4).
                    is ResultadoDuelo.Rechazado -> estado.copy(cargando = false, cargandoMas = false, error = R.string.ranking_error_no_inscrito)
                    ResultadoDuelo.SinConexion -> estado.copy(cargando = false, cargandoMas = false, error = R.string.duelos_error_conexion)
                    ResultadoDuelo.SesionVencida -> estado.copy(cargando = false, cargandoMas = false, sesionVencida = true)
                    ResultadoDuelo.ErrorInesperado -> estado.copy(cargando = false, cargandoMas = false, error = R.string.duelos_error_inesperado)
                }
            }
        }
    }

    companion object {
        fun factory(misCursos: MisCursosRepository, duelos: DuelosRepository) = viewModelFactory {
            initializer { RankingViewModel(misCursos, duelos) }
        }
    }
}
