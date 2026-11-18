package com.mastercode.app.ui.racha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mastercode.app.data.racha.RachaRepository
import com.mastercode.app.data.racha.ResultadoRacha
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Qué le dice la pantalla al estudiante según su racha.
enum class MensajeDeRacha {
    // No tiene racha: invita a empezar una.
    Empezar,

    // Tiene racha y todavía no practicó hoy: la pierde si no practica.
    PracticarHoy,

    // Tiene racha y ya practicó hoy.
    YaPractico,
}

// Lo que muestra la racha en la pantalla principal. Oculta: no se pudo saber (sin conexión, error o sesión vencida); la
// racha es un adorno de la pantalla y no debe estorbar ni mostrar un error: «Mis cursos» ya avisa de sus propios fallos.
sealed interface EstadoDeRacha {
    data object Cargando : EstadoDeRacha
    data class ConRacha(val rachaActual: Int, val practicoHoy: Boolean) : EstadoDeRacha {
        val mensaje: MensajeDeRacha
            get() = when {
                rachaActual <= 0 -> MensajeDeRacha.Empezar
                practicoHoy -> MensajeDeRacha.YaPractico
                else -> MensajeDeRacha.PracticarHoy
            }
    }
    data object Oculta : EstadoDeRacha
}

// HU49-T3 (CA-49.3): la racha actual en la pantalla principal.
class RachaViewModel(private val repository: RachaRepository) : ViewModel() {
    private val _estado = MutableStateFlow<EstadoDeRacha>(EstadoDeRacha.Cargando)
    val estado: StateFlow<EstadoDeRacha> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = when (val resultado = repository.racha()) {
                is ResultadoRacha.Exito -> EstadoDeRacha.ConRacha(resultado.racha.rachaActual, resultado.racha.practicoHoy)
                ResultadoRacha.SesionVencida, ResultadoRacha.SinConexion, ResultadoRacha.ErrorInesperado -> EstadoDeRacha.Oculta
            }
        }
    }

    companion object {
        fun factory(repository: RachaRepository) = viewModelFactory {
            initializer { RachaViewModel(repository) }
        }
    }
}
