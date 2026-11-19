package com.mastercode.app.ui.avisos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mastercode.app.data.avisos.AjustesDeAvisos
import com.mastercode.app.data.avisos.AlmacenDeAvisos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// HU52-T3: activar o desactivar los avisos de práctica y elegir a qué hora llegan. Cada cambio queda guardado en el
// teléfono al instante y, además, se avisa con alCambiar para mandarlo a user-api (HU52-T4).
class AvisosViewModel(
    private val almacen: AlmacenDeAvisos,
    private val alCambiar: (AjustesDeAvisos) -> Unit = {},
) : ViewModel() {
    private val _ajustes = MutableStateFlow(almacen.leer())
    val ajustes: StateFlow<AjustesDeAvisos> = _ajustes.asStateFlow()

    fun activar(activados: Boolean) = cambiar(_ajustes.value.copy(activados = activados))

    // Una hora fuera de rango se ignora.
    fun elegirHora(hora: Int, minuto: Int) {
        if (AjustesDeAvisos.horaValida(hora, minuto)) cambiar(_ajustes.value.copy(hora = hora, minuto = minuto))
    }

    private fun cambiar(nuevos: AjustesDeAvisos) {
        _ajustes.value = nuevos
        almacen.guardar(nuevos)
        alCambiar(nuevos)
    }

    companion object {
        fun factory(almacen: AlmacenDeAvisos, alCambiar: (AjustesDeAvisos) -> Unit = {}) = viewModelFactory {
            initializer { AvisosViewModel(almacen, alCambiar) }
        }
    }
}
