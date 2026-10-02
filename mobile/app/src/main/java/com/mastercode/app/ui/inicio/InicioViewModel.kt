package com.mastercode.app.ui.inicio

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class InicioUiState(val toques: Int = 0)

class InicioViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(InicioUiState())
    val uiState: StateFlow<InicioUiState> = _uiState.asStateFlow()

    fun alTocar() {
        _uiState.update { it.copy(toques = it.toques + 1) }
    }
}
