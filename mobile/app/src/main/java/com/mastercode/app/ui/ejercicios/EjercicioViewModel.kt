package com.mastercode.app.ui.ejercicios

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mastercode.app.R
import com.mastercode.app.data.problemas.ProblemaDelEjercicio
import com.mastercode.app.data.problemas.ProblemaRepository
import com.mastercode.app.data.problemas.RespuestaDelEstudiante
import com.mastercode.app.data.problemas.ResultadoDeEnvio
import com.mastercode.app.data.problemas.ResultadoProblema
import com.mastercode.app.data.respuestas.EnviadorDeRespuestas
import com.mastercode.app.data.respuestas.PendienteResuelto
import com.mastercode.app.data.respuestas.SalidaDeEnvio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

const val TIPO_PREDICCION_SALIDA = "prediccion_salida"
const val TIPO_COMPLETAR_ESPACIO = "completar_espacio"
const val TIPO_OPCION_MULTIPLE = "opcion_multiple"

// Alto mínimo de cada botón de alternativa (HU46-T3, CA-46.5): las guías de accesibilidad piden 48 dp como mínimo para
// que se pueda tocar sin errar; se usa 56 para dejar margen.
const val ALTURA_MINIMA_OPCION_DP = 56

// Lo que muestra la pantalla: solo uno de estos casos a la vez.
sealed interface ContenidoEjercicio {
    data object Cargando : ContenidoEjercicio
    data class Listo(val problema: ProblemaDelEjercicio) : ContenidoEjercicio
    data class Error(@param:StringRes val mensaje: Int) : ContenidoEjercicio
    data object SesionVencida : ContenidoEjercicio
}

// En qué va la respuesta del estudiante (HU45-T3, HU48-T2, HU48-T3).
sealed interface EstadoDeEnvio {
    data object SinEnviar : EstadoDeEnvio
    data object Enviando : EstadoDeEnvio

    // Corregida: si acertó y, solo si falló, cuál era la correcta (CA-48.2, CA-48.3).
    data class Resultado(val acerto: Boolean, val respuestaCorrecta: String?) : EstadoDeEnvio

    // Sin conexión: quedó guardada en el teléfono y se enviará cuando vuelva la señal (CA-48.4).
    data object Pendiente : EstadoDeEnvio

    // No se pudo enviar ni corregir; el estudiante puede volver a intentar.
    data class Rechazado(@param:StringRes val mensaje: Int) : EstadoDeEnvio
}

// respuesta es lo que el estudiante lleva escrito y opcionElegida la alternativa que tocó. enSesion: el ejercicio se
// responde dentro de una sesión del Dojo, donde cada ejercicio se responde una sola vez.
data class EjercicioUiState(
    val contenido: ContenidoEjercicio = ContenidoEjercicio.Cargando,
    val respuesta: String = "",
    val opcionElegida: Long? = null,
    val envio: EstadoDeEnvio = EstadoDeEnvio.SinEnviar,
    val enSesion: Boolean = false,
) {
    // Mientras se envía, o ya con resultado o guardada, ya no se cambia lo contestado.
    val editable: Boolean get() = envio == EstadoDeEnvio.SinEnviar || envio is EstadoDeEnvio.Rechazado

    // Los intentos que le quedan en este ejercicio (HU45-T3); se actualiza con cada respuesta corregida.
    val intentosRestantes: Int? get() = (contenido as? ContenidoEjercicio.Listo)?.problema?.intentosRestantes

    val puedeEnviar: Boolean
        get() {
            val problema = (contenido as? ContenidoEjercicio.Listo)?.problema ?: return false
            if (!editable) return false
            return if (problema.tipoEjercicio == TIPO_OPCION_MULTIPLE) opcionElegida != null else respuesta.isNotBlank()
        }

    // Fuera del Dojo, tras fallar con intentos por gastar se puede intentar otra vez (CA-45.5).
    val puedeIntentarDeNuevo: Boolean
        get() = !enSesion && envio is EstadoDeEnvio.Resultado && !envio.acerto && (intentosRestantes ?: 0) > 0
}

// Sirve a las tres pantallas («predecir la salida», «completar el espacio» y «opción múltiple»): cargan, guardan lo
// contestado y envían igual, y solo se dibujan distinto. tipoEsperado evita mostrar un ejercicio de otro tipo en la
// pantalla equivocada. Si el Dojo ya trajo el ejercicio, llega en precargado y no se vuelve a pedir; sesionId se manda con
// la respuesta para que el Dojo la cuente (HU54).
class EjercicioViewModel(
    private val repository: ProblemaRepository,
    private val enviador: EnviadorDeRespuestas,
    private val problemaId: Long,
    private val tipoEsperado: String,
    private val sesionId: Long? = null,
    precargado: ProblemaDelEjercicio? = null,
    private val nuevoIntentoId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(EjercicioUiState(enSesion = sesionId != null))
    val uiState: StateFlow<EjercicioUiState> = _uiState.asStateFlow()

    // El identificador del intento en curso: se reusa si el envío se repite, y se cambia al recibir la corrección.
    private var intentoActual: String? = null

    init {
        // Si ya hay una respuesta de este ejercicio esperando señal (se salió y se volvió a entrar), se retoma.
        val guardada = enviador.pendienteDe(problemaId)
        if (guardada != null) {
            intentoActual = guardada.intentoId
            _uiState.update {
                it.copy(respuesta = guardada.texto ?: "", opcionElegida = guardada.opcionId, envio = EstadoDeEnvio.Pendiente)
            }
        }
        if (precargado != null) {
            _uiState.update { it.copy(contenido = contenidoDe(precargado)) }
        } else {
            cargar()
        }
        // Cuando la respuesta guardada por fin se envía, la pantalla muestra el resultado.
        viewModelScope.launch {
            enviador.resueltas.collect { resuelto ->
                if (resuelto.respuesta.intentoId == intentoActual) alResolverseElPendiente(resuelto)
            }
        }
    }

    // También es la acción de «Reintentar».
    fun cargar() {
        _uiState.update { it.copy(contenido = ContenidoEjercicio.Cargando) }
        viewModelScope.launch {
            val contenido = when (val resultado = repository.problema(problemaId)) {
                is ResultadoProblema.Exito -> contenidoDe(resultado.problema)
                ResultadoProblema.NoEncontrado -> ContenidoEjercicio.Error(R.string.ejercicio_error_no_encontrado)
                ResultadoProblema.SesionVencida -> ContenidoEjercicio.SesionVencida
                ResultadoProblema.SinConexion -> ContenidoEjercicio.Error(R.string.ejercicio_error_conexion)
                ResultadoProblema.ErrorInesperado -> ContenidoEjercicio.Error(R.string.ejercicio_error_inesperado)
            }
            _uiState.update { it.copy(contenido = contenido) }
        }
    }

    private fun contenidoDe(problema: ProblemaDelEjercicio): ContenidoEjercicio =
        if (problema.tipoEjercicio == tipoEsperado) {
            ContenidoEjercicio.Listo(problema)
        } else {
            ContenidoEjercicio.Error(R.string.ejercicio_error_tipo)
        }

    fun alCambiarRespuesta(texto: String) {
        if (_uiState.value.editable) _uiState.update { it.copy(respuesta = texto) }
    }

    fun alElegirOpcion(opcionId: Long) {
        if (_uiState.value.editable) _uiState.update { it.copy(opcionElegida = opcionId) }
    }

    // HU45-T3, HU48-T2: manda lo contestado. Con conexión, la pantalla muestra al instante si acertó; sin ella, queda
    // guardado en el teléfono (HU48-T3).
    fun enviar() {
        val estado = _uiState.value
        if (!estado.puedeEnviar) return
        val intentoId = intentoActual ?: nuevoIntentoId().also { intentoActual = it }
        val porOpcion = estado.opcionElegida != null
        val respuesta = RespuestaDelEstudiante(
            problemaId = problemaId,
            texto = if (porOpcion) null else estado.respuesta,
            opcionId = estado.opcionElegida,
            sesionId = sesionId,
            intentoId = intentoId,
        )
        _uiState.update { it.copy(envio = EstadoDeEnvio.Enviando) }
        viewModelScope.launch {
            when (val salida = enviador.enviar(respuesta)) {
                is SalidaDeEnvio.Enviada -> {
                    intentoActual = null
                    _uiState.update { conCorreccion(it, salida.resultado.acerto, salida.resultado.respuestaCorrecta, salida.resultado.intentosRestantes) }
                }
                SalidaDeEnvio.Guardada -> _uiState.update { it.copy(envio = EstadoDeEnvio.Pendiente) }
                is SalidaDeEnvio.Fallida -> _uiState.update { it.copy(envio = rechazoDe(salida.motivo)) }
                SalidaDeEnvio.SesionVencida -> _uiState.update {
                    it.copy(contenido = ContenidoEjercicio.SesionVencida, envio = EstadoDeEnvio.SinEnviar)
                }
            }
        }
    }

    // Tras fallar con intentos por gastar: deja el ejercicio listo para otra respuesta, con otro identificador de intento.
    fun intentarDeNuevo() {
        if (!_uiState.value.puedeIntentarDeNuevo) return
        intentoActual = null
        _uiState.update { it.copy(respuesta = "", opcionElegida = null, envio = EstadoDeEnvio.SinEnviar) }
    }

    private fun alResolverseElPendiente(resuelto: PendienteResuelto) {
        val resultado = resuelto.resultado
        if (resultado is ResultadoDeEnvio.Enviada) {
            intentoActual = null
            val r = resultado.resultado
            _uiState.update { conCorreccion(it, r.acerto, r.respuestaCorrecta, r.intentosRestantes) }
        } else {
            _uiState.update { it.copy(envio = rechazoDe(resultado)) }
        }
    }

    // La corrección también actualiza los intentos que quedan en el ejercicio que se muestra.
    private fun conCorreccion(estado: EjercicioUiState, acerto: Boolean, correcta: String?, restantes: Int): EjercicioUiState {
        val contenido = estado.contenido
        return estado.copy(
            contenido = if (contenido is ContenidoEjercicio.Listo) {
                ContenidoEjercicio.Listo(contenido.problema.copy(intentosRestantes = restantes))
            } else {
                contenido
            },
            envio = EstadoDeEnvio.Resultado(acerto, correcta),
        )
    }

    private fun rechazoDe(motivo: ResultadoDeEnvio): EstadoDeEnvio.Rechazado = EstadoDeEnvio.Rechazado(
        when (motivo) {
            ResultadoDeEnvio.Conflicto -> R.string.ejercicio_rechazado_conflicto
            ResultadoDeEnvio.Invalida -> R.string.ejercicio_rechazado_invalida
            ResultadoDeEnvio.NoEncontrado -> R.string.ejercicio_error_no_encontrado
            else -> R.string.ejercicio_rechazado_inesperado
        },
    )

    companion object {
        fun factory(
            repository: ProblemaRepository,
            enviador: EnviadorDeRespuestas,
            problemaId: Long,
            tipoEsperado: String,
            sesionId: Long? = null,
            precargado: ProblemaDelEjercicio? = null,
        ) = viewModelFactory {
            initializer { EjercicioViewModel(repository, enviador, problemaId, tipoEsperado, sesionId, precargado) }
        }
    }
}
