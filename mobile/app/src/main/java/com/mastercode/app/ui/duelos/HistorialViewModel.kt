package com.mastercode.app.ui.duelos

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mastercode.app.R
import com.mastercode.app.data.duelos.DuelosRepository
import com.mastercode.app.data.duelos.PartidaDelHistorial
import com.mastercode.app.data.duelos.ResultadoDuelo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val TAMANO_DE_PAGINA = 20

// partidas son las páginas ya traídas, una tras otra. hayMas: user-api tiene más páginas. cargando: la primera página.
// cargandoMas: una página siguiente. error: por qué falló la última carga (lo ya traído se conserva).
data class HistorialUiState(
    val partidas: List<PartidaDelHistorial> = emptyList(),
    val pagina: Int = 0,
    val total: Int = 0,
    val cargando: Boolean = true,
    val cargandoMas: Boolean = false,
    @param:StringRes val error: Int? = null,
    val sesionVencida: Boolean = false,
) {
    val hayMas: Boolean get() = partidas.size < total
    val vacio: Boolean get() = !cargando && error == null && partidas.isEmpty()
}

// HU60-T3: el historial de partidas del estudiante, la más reciente primero (como la ordena user-api), de a 20.
class HistorialViewModel(private val duelos: DuelosRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    // La primera página; también es «Reintentar» y actualizar.
    fun cargar() {
        _uiState.update { HistorialUiState(cargando = true) }
        pedir(1)
    }

    fun cargarMas() {
        val estado = _uiState.value
        if (estado.cargando || estado.cargandoMas || !estado.hayMas) return
        _uiState.update { it.copy(cargandoMas = true, error = null) }
        pedir(estado.pagina + 1)
    }

    private fun pedir(pagina: Int) {
        viewModelScope.launch {
            val resultado = duelos.historial(pagina, TAMANO_DE_PAGINA)
            _uiState.update {
                when (resultado) {
                    is ResultadoDuelo.Exito -> it.copy(
                        partidas = if (pagina == 1) resultado.valor.items else it.partidas + resultado.valor.items,
                        pagina = pagina,
                        total = resultado.valor.total,
                        cargando = false,
                        cargandoMas = false,
                        error = null,
                    )
                    ResultadoDuelo.SinConexion -> it.copy(cargando = false, cargandoMas = false, error = R.string.duelos_error_conexion)
                    ResultadoDuelo.SesionVencida -> it.copy(cargando = false, cargandoMas = false, sesionVencida = true)
                    else -> it.copy(cargando = false, cargandoMas = false, error = R.string.duelos_error_inesperado)
                }
            }
        }
    }

    companion object {
        fun factory(duelos: DuelosRepository) = viewModelFactory {
            initializer { HistorialViewModel(duelos) }
        }
    }
}
