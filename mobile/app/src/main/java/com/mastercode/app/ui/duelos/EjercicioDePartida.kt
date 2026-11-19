package com.mastercode.app.ui.duelos

import androidx.annotation.StringRes
import com.mastercode.app.data.problemas.ProblemaDelEjercicio
import com.mastercode.app.ui.ejercicios.ContenidoEjercicio
import com.mastercode.app.ui.ejercicios.ControlDeEjercicio
import com.mastercode.app.ui.ejercicios.EjercicioUiState
import com.mastercode.app.ui.ejercicios.EstadoDeEnvio
import com.mastercode.app.ui.ejercicios.TIPO_OPCION_MULTIPLE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Lo que pasó al mandar la respuesta de un turno de la partida.
sealed interface SalidaDeRespuesta {
    data class Corregida(val acerto: Boolean, val respuestaCorrecta: String?) : SalidaDeRespuesta
    data class Rechazada(@param:StringRes val mensaje: Int) : SalidaDeRespuesta
    data object SesionVencida : SalidaDeRespuesta
}

// H8: un ejercicio de la partida con la misma interfaz que el de los problemas sueltos y el del Dojo (ControlDeEjercicio),
// así se dibuja con las mismas pantallas. Cambia a dónde va la respuesta: la corrige user-api dentro de la partida
// (alResponder), sin guardarla en el teléfono ni permitir reintentos: en un duelo cada turno se responde una sola vez.
class EjercicioDePartida(
    private val problema: ProblemaDelEjercicio,
    val turno: Int,
    private val ambito: CoroutineScope,
    private val alResponder: suspend (turno: Int, texto: String?, opcionId: Long?) -> SalidaDeRespuesta,
) : ControlDeEjercicio {
    private val _uiState = MutableStateFlow(EjercicioUiState(contenido = ContenidoEjercicio.Listo(problema), enSesion = true))
    override val uiState: StateFlow<EjercicioUiState> = _uiState.asStateFlow()

    // El ejercicio ya viene cargado: no hay nada que reintentar.
    override fun cargar() {}

    override fun alCambiarRespuesta(texto: String) {
        if (_uiState.value.editable) _uiState.update { it.copy(respuesta = texto) }
    }

    override fun alElegirOpcion(opcionId: Long) {
        if (_uiState.value.editable) _uiState.update { it.copy(opcionElegida = opcionId) }
    }

    override fun enviar() {
        val estado = _uiState.value
        if (!estado.puedeEnviar) return
        val porOpcion = problema.tipoEjercicio == TIPO_OPCION_MULTIPLE
        _uiState.update { it.copy(envio = EstadoDeEnvio.Enviando) }
        ambito.launch {
            val salida = alResponder(turno, if (porOpcion) null else estado.respuesta, if (porOpcion) estado.opcionElegida else null)
            _uiState.update {
                when (salida) {
                    is SalidaDeRespuesta.Corregida -> it.copy(envio = EstadoDeEnvio.Resultado(salida.acerto, salida.respuestaCorrecta))
                    is SalidaDeRespuesta.Rechazada -> it.copy(envio = EstadoDeEnvio.Rechazado(salida.mensaje))
                    SalidaDeRespuesta.SesionVencida -> it.copy(contenido = ContenidoEjercicio.SesionVencida, envio = EstadoDeEnvio.SinEnviar)
                }
            }
        }
    }

    // Un duelo no deja reintentar un turno.
    override fun intentarDeNuevo() {}
}
