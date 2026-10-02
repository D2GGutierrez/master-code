package com.mastercode.app.ui.inicio

import org.junit.Assert.assertEquals
import org.junit.Test

class InicioViewModelTest {
    @Test
    fun estadoInicialSinToques() {
        val viewModel = InicioViewModel()

        assertEquals(0, viewModel.uiState.value.toques)
    }

    @Test
    fun alTocarDosVecesSumaDos() {
        val viewModel = InicioViewModel()

        viewModel.alTocar()
        viewModel.alTocar()

        assertEquals(2, viewModel.uiState.value.toques)
    }
}
