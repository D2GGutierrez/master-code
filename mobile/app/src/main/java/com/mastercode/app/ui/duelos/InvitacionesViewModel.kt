package com.mastercode.app.ui.duelos

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mastercode.app.R
import com.mastercode.app.data.duelos.DuelosRepository
import com.mastercode.app.data.duelos.Invitacion
import com.mastercode.app.data.duelos.ResultadoDuelo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ContenidoDeInvitaciones {
    data object Cargando : ContenidoDeInvitaciones
    data class Lista(val invitaciones: List<Invitacion>) : ContenidoDeInvitaciones
    data object Vacio : ContenidoDeInvitaciones
    data class Error(@param:StringRes val mensaje: Int) : ContenidoDeInvitaciones
}

// procesando: la invitación que se está aceptando o rechazando. partidaAceptada: la partida a abrir; la pantalla avisa con
// alAbrirseLaPartida. mensaje: por qué no se pudo aceptar o rechazar.
data class InvitacionesUiState(
    val contenido: ContenidoDeInvitaciones = ContenidoDeInvitaciones.Cargando,
    val procesando: Long? = null,
    @param:StringRes val mensaje: Int? = null,
    val partidaAceptada: Long? = null,
    val sesionVencida: Boolean = false,
)

// H7 en la app: los retos que le hicieron al estudiante. Aceptar abre la partida; rechazar la quita de la lista.
class InvitacionesViewModel(private val duelos: DuelosRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(InvitacionesUiState())
    val uiState: StateFlow<InvitacionesUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    // También es la acción de «Reintentar» y de actualizar la lista.
    fun cargar() {
        _uiState.update { it.copy(contenido = ContenidoDeInvitaciones.Cargando, mensaje = null) }
        viewModelScope.launch {
            _uiState.update {
                when (val resultado = duelos.invitaciones()) {
                    is ResultadoDuelo.Exito ->
                        it.copy(contenido = if (resultado.valor.isEmpty()) ContenidoDeInvitaciones.Vacio else ContenidoDeInvitaciones.Lista(resultado.valor))
                    is ResultadoDuelo.Rechazado, ResultadoDuelo.ErrorInesperado ->
                        it.copy(contenido = ContenidoDeInvitaciones.Error(R.string.duelos_error_inesperado))
                    ResultadoDuelo.SinConexion -> it.copy(contenido = ContenidoDeInvitaciones.Error(R.string.duelos_error_conexion))
                    ResultadoDuelo.SesionVencida -> it.copy(sesionVencida = true)
                }
            }
        }
    }

    fun aceptar(invitacion: Invitacion) {
        if (_uiState.value.procesando != null) return
        _uiState.update { it.copy(procesando = invitacion.id, mensaje = null) }
        viewModelScope.launch {
            when (val resultado = duelos.aceptar(invitacion.id)) {
                is ResultadoDuelo.Exito -> _uiState.update { it.copy(procesando = null, partidaAceptada = invitacion.id) }
                is ResultadoDuelo.Rechazado -> {
                    _uiState.update { it.copy(procesando = null, mensaje = mensajeDeAceptar(resultado.codigo)) }
                    // Caducó o ya estaba respondida: la lista se actualiza para que desaparezca.
                    if (resultado.codigo == 409 || resultado.codigo == 404) quitar(invitacion.id)
                }
                ResultadoDuelo.SinConexion -> _uiState.update { it.copy(procesando = null, mensaje = R.string.duelos_error_conexion) }
                ResultadoDuelo.SesionVencida -> _uiState.update { it.copy(procesando = null, sesionVencida = true) }
                ResultadoDuelo.ErrorInesperado -> _uiState.update { it.copy(procesando = null, mensaje = R.string.duelos_error_inesperado) }
            }
        }
    }

    fun rechazar(invitacion: Invitacion) {
        if (_uiState.value.procesando != null) return
        _uiState.update { it.copy(procesando = invitacion.id, mensaje = null) }
        viewModelScope.launch {
            when (val resultado = duelos.rechazar(invitacion.id)) {
                is ResultadoDuelo.Exito -> {
                    _uiState.update { it.copy(procesando = null) }
                    quitar(invitacion.id)
                }
                is ResultadoDuelo.Rechazado -> {
                    _uiState.update { it.copy(procesando = null, mensaje = mensajeDeAceptar(resultado.codigo)) }
                    if (resultado.codigo == 409 || resultado.codigo == 404) quitar(invitacion.id)
                }
                ResultadoDuelo.SinConexion -> _uiState.update { it.copy(procesando = null, mensaje = R.string.duelos_error_conexion) }
                ResultadoDuelo.SesionVencida -> _uiState.update { it.copy(procesando = null, sesionVencida = true) }
                ResultadoDuelo.ErrorInesperado -> _uiState.update { it.copy(procesando = null, mensaje = R.string.duelos_error_inesperado) }
            }
        }
    }

    // La pantalla ya abrió la partida aceptada.
    fun alAbrirseLaPartida() {
        _uiState.update { it.copy(partidaAceptada = null) }
    }

    private fun quitar(invitacionId: Long) {
        _uiState.update { estado ->
            val lista = (estado.contenido as? ContenidoDeInvitaciones.Lista)?.invitaciones ?: return@update estado
            val quedan = lista.filter { it.id != invitacionId }
            estado.copy(contenido = if (quedan.isEmpty()) ContenidoDeInvitaciones.Vacio else ContenidoDeInvitaciones.Lista(quedan))
        }
    }

    @StringRes
    private fun mensajeDeAceptar(codigo: Int): Int = when (codigo) {
        409 -> R.string.invitaciones_caducada
        403, 404 -> R.string.invitaciones_no_disponible
        else -> R.string.duelos_error_inesperado
    }

    companion object {
        fun factory(duelos: DuelosRepository) = viewModelFactory {
            initializer { InvitacionesViewModel(duelos) }
        }
    }
}
