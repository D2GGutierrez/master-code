package com.mastercode.app.ui.duelos

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mastercode.app.R
import com.mastercode.app.data.duelos.DuelosRepository
import com.mastercode.app.data.duelos.EjercicioDelTurno
import com.mastercode.app.data.duelos.Marcador
import com.mastercode.app.data.duelos.ResultadoDuelo
import com.mastercode.app.ui.ejercicios.ControlDeEjercicio
import com.mastercode.app.ui.ejercicios.EstadoDeEnvio
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// En qué momento de la partida está el estudiante. El marcador no es una fase: se ve siempre, arriba (HU59-T3).
sealed interface FaseDePartida {
    data object Cargando : FaseDePartida

    // El reto sigue pendiente: el rival todavía no lo acepta.
    data object EsperandoRival : FaseDePartida

    // Un ejercicio del turno, con su control (el mismo que usan las pantallas de ejercicio).
    data class Jugando(val ejercicio: ControlDeEjercicio, val turno: Int, val total: Int, val tipo: String) : FaseDePartida

    // Ya respondió todos sus ejercicios y falta que el rival termine.
    data object EsperandoAlRival : FaseDePartida

    data class Terminada(val marcador: Marcador) : FaseDePartida

    // La partida no sigue: el reto fue rechazado o caducó, o no existe.
    data class Cerrada(@param:StringRes val mensaje: Int) : FaseDePartida

    data class Error(@param:StringRes val mensaje: Int) : FaseDePartida
    data object SesionVencida : FaseDePartida
}

// sinSenal: la última consulta del marcador no llegó. El marcador y el ejercicio siguen en pantalla y se vuelve a intentar.
data class PartidaUiState(
    val fase: FaseDePartida = FaseDePartida.Cargando,
    val marcador: Marcador? = null,
    val sinSenal: Boolean = false,
) {
    // El ejercicio del turno ya tiene su corrección (la pantalla muestra entonces «Siguiente»). Es un valor leído en el
    // momento: la pantalla observa además el estado del propio ejercicio para redibujarse.
    val respondido: Boolean get() = (fase as? FaseDePartida.Jugando)?.ejercicio?.uiState?.value?.envio is EstadoDeEnvio.Resultado
}

// H8 y HU59-T3: la partida de un duelo. Sirve al retador (que primero espera la respuesta del rival) y a quien aceptó: las
// dos cosas arrancan igual, consultando el marcador. Esa consulta se repite cada 2 s (D20) con el ETag de la anterior, así
// user-api contesta 304 si nada cambió; además es la señal con la que user-api sabe que el jugador sigue conectado. Por eso
// la pantalla la enciende solo mientras está a la vista (reanudar) y la apaga al salir o al pasar la app a segundo plano
// (pausar): un jugador que se va debe dejar de dar señal para que, a los 30 s, la partida se dé por abandonada. Si una
// consulta falla (sin señal o muy lenta) se conserva lo que ya se ve y se reintenta.
// esperar es la pausa entre consultas; solo las pruebas la reemplazan.
class PartidaViewModel(
    private val repository: DuelosRepository,
    private val partidaId: Long,
    private val esperar: suspend () -> Unit = { delay(INTERVALO_DEL_MARCADOR_MS) },
) : ViewModel() {
    private val _uiState = MutableStateFlow(PartidaUiState())
    val uiState: StateFlow<PartidaUiState> = _uiState.asStateFlow()

    private var etag: String? = null
    private var cargandoEjercicio = false
    private var sondeo: Job? = null

    // Empieza a consultar el marcador (la primera consulta es inmediata). No hace nada si ya está consultando o si la partida
    // ya terminó.
    fun reanudar() {
        if (sondeo?.isActive == true || terminoElSondeo()) return
        sondeo = viewModelScope.launch {
            while (isActive && !terminoElSondeo()) {
                sondear()
                esperar()
            }
        }
    }

    // Deja de consultar: el jugador sale de la pantalla o la app pasa a segundo plano.
    fun pausar() {
        sondeo?.cancel()
        sondeo = null
    }

    private fun terminoElSondeo(): Boolean = when (_uiState.value.fase) {
        is FaseDePartida.Terminada, is FaseDePartida.Cerrada, FaseDePartida.SesionVencida -> true
        else -> false
    }

    // Una consulta del marcador. Con 304 no cambia nada salvo que ya hay señal.
    suspend fun sondear() {
        when (val resultado = repository.marcador(partidaId, etag)) {
            is ResultadoDuelo.Exito -> {
                etag = resultado.valor.etag
                _uiState.update { it.copy(sinSenal = false) }
                resultado.valor.marcador?.let { alLlegarElMarcador(it) }
            }
            // 403 y 404: la partida no existe o no es de este estudiante. Cualquier otro rechazo (por ejemplo un 503) es
            // pasajero, como la falta de señal.
            is ResultadoDuelo.Rechazado ->
                if (resultado.codigo == 403 || resultado.codigo == 404) {
                    _uiState.update { it.copy(fase = FaseDePartida.Cerrada(R.string.partida_no_existe), sinSenal = false) }
                } else {
                    _uiState.update { it.copy(sinSenal = true) }
                }
            ResultadoDuelo.SesionVencida -> _uiState.update { it.copy(fase = FaseDePartida.SesionVencida) }
            // Sin señal o error pasajero: se conserva lo que ya se ve y el sondeo lo vuelve a intentar.
            ResultadoDuelo.SinConexion, ResultadoDuelo.ErrorInesperado -> _uiState.update { it.copy(sinSenal = true) }
        }
    }

    private suspend fun alLlegarElMarcador(marcador: Marcador) {
        _uiState.update { it.copy(marcador = marcador) }
        when (marcador.estado) {
            "pendiente" -> _uiState.update { if (it.fase == FaseDePartida.Cargando) it.copy(fase = FaseDePartida.EsperandoRival) else it }
            "en_curso" -> {
                val fase = _uiState.value.fase
                if (fase == FaseDePartida.Cargando || fase == FaseDePartida.EsperandoRival) cargarEjercicio()
            }
            "terminada" -> terminarSiYaNoSeEstaViendoUnResultado(marcador)
            "rechazada" -> _uiState.update { it.copy(fase = FaseDePartida.Cerrada(R.string.partida_rechazada)) }
            else -> _uiState.update { it.copy(fase = FaseDePartida.Cerrada(R.string.partida_caducada)) }
        }
    }

    // Si el estudiante está viendo la corrección de su última respuesta, el resultado espera a que toque «Siguiente».
    private fun terminarSiYaNoSeEstaViendoUnResultado(marcador: Marcador) {
        if (!_uiState.value.respondido) _uiState.update { it.copy(fase = FaseDePartida.Terminada(marcador)) }
    }

    // Pide el ejercicio que toca. 204: ya respondió todos y espera al rival.
    suspend fun cargarEjercicio() {
        if (cargandoEjercicio) return
        cargandoEjercicio = true
        try {
            when (val resultado = repository.ejercicio(partidaId)) {
                is ResultadoDuelo.Exito -> {
                    val entrega = resultado.valor
                    _uiState.update { it.copy(fase = entrega?.let(::jugando) ?: FaseDePartida.EsperandoAlRival) }
                }
                // La partida ya no está en curso (el rival abandonó o terminó): el marcador lo cuenta en la próxima consulta.
                is ResultadoDuelo.Rechazado -> etag = null
                ResultadoDuelo.SesionVencida -> _uiState.update { it.copy(fase = FaseDePartida.SesionVencida) }
                ResultadoDuelo.SinConexion -> _uiState.update { it.copy(fase = FaseDePartida.Error(R.string.partida_error_conexion)) }
                ResultadoDuelo.ErrorInesperado -> _uiState.update { it.copy(fase = FaseDePartida.Error(R.string.partida_error_inesperado)) }
            }
        } finally {
            cargandoEjercicio = false
        }
    }

    private fun jugando(entrega: EjercicioDelTurno): FaseDePartida.Jugando = FaseDePartida.Jugando(
        ejercicio = EjercicioDePartida(entrega.problema, entrega.turno, viewModelScope, ::responder),
        turno = entrega.turno,
        total = entrega.totalEjercicios,
        tipo = entrega.problema.tipoEjercicio,
    )

    // La acción de «Reintentar» tras un error al pedir el ejercicio.
    fun reintentar() {
        viewModelScope.launch { cargarEjercicio() }
    }

    // «Siguiente»: el ejercicio que sigue, o el resultado si la partida ya terminó.
    fun siguiente() {
        val marcador = _uiState.value.marcador
        if (marcador != null && marcador.estado == "terminada") {
            _uiState.update { it.copy(fase = FaseDePartida.Terminada(marcador)) }
        } else {
            viewModelScope.launch { cargarEjercicio() }
        }
    }

    // Manda la respuesta del turno. El marcador que devuelve user-api se muestra de inmediato, sin esperar a la próxima consulta.
    private suspend fun responder(turno: Int, texto: String?, opcionId: Long?): SalidaDeRespuesta =
        when (val resultado = repository.responder(partidaId, turno, texto, opcionId)) {
            is ResultadoDuelo.Exito -> {
                val respuesta = resultado.valor
                _uiState.update { it.copy(marcador = respuesta.marcador, sinSenal = false) }
                SalidaDeRespuesta.Corregida(respuesta.acerto, respuesta.respuestaCorrecta)
            }
            is ResultadoDuelo.Rechazado -> when (resultado.codigo) {
                // El turno ya no es ese (por ejemplo, la respuesta sí llegó pero se perdió la contestación): se vuelve a
                // pedir el ejercicio que user-api espera y se sigue desde ahí.
                409 -> {
                    etag = null
                    viewModelScope.launch { cargarEjercicio() }
                    SalidaDeRespuesta.Rechazada(R.string.partida_turno_ya_respondido)
                }
                else -> SalidaDeRespuesta.Rechazada(R.string.partida_respuesta_invalida)
            }
            ResultadoDuelo.SesionVencida -> SalidaDeRespuesta.SesionVencida
            ResultadoDuelo.SinConexion -> SalidaDeRespuesta.Rechazada(R.string.partida_error_conexion_respuesta)
            ResultadoDuelo.ErrorInesperado -> SalidaDeRespuesta.Rechazada(R.string.partida_error_inesperado)
        }

    companion object {
        const val INTERVALO_DEL_MARCADOR_MS = 2_000L

        fun factory(repository: DuelosRepository, partidaId: Long) = viewModelFactory {
            initializer { PartidaViewModel(repository, partidaId) }
        }
    }
}
