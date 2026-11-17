package com.mastercode.app.data.respuestas

import com.mastercode.app.data.problemas.ProblemaRepository
import com.mastercode.app.data.problemas.RespuestaDelEstudiante
import com.mastercode.app.data.problemas.ResultadoDeEnvio
import com.mastercode.app.data.problemas.ResultadoDeRespuesta
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Qué pasó con una respuesta recién enviada. Guardada: no había conexión y quedó en el teléfono (HU48-T3).
sealed interface SalidaDeEnvio {
    data class Enviada(val resultado: ResultadoDeRespuesta) : SalidaDeEnvio
    data object Guardada : SalidaDeEnvio
    // El servidor la rechazó (Conflicto, Invalida, NoEncontrado) o falló (ErrorInesperado): no se guarda para después.
    data class Fallida(val motivo: ResultadoDeEnvio) : SalidaDeEnvio
    data object SesionVencida : SalidaDeEnvio
}

// Una respuesta guardada que por fin se envió; resultado es lo que contestó el servidor.
data class PendienteResuelto(val respuesta: RespuestaDelEstudiante, val resultado: ResultadoDeEnvio)

// HU48-T3: envía las respuestas y, si no hay conexión, las guarda en el teléfono para reenviarlas cuando vuelva la señal.
// Cada intento lleva su intentoId; user-api lo reconoce, así que un reenvío de algo que sí había llegado no se cuenta dos
// veces y la respuesta queda registrada una sola vez (CA-48.5).
class EnviadorDeRespuestas(
    private val repository: ProblemaRepository,
    private val almacen: AlmacenDePendientes,
) {
    // Un solo reenvío a la vez, y nadie cambia la lista guardada mientras tanto.
    private val candado = Mutex()

    private val _resueltas = MutableSharedFlow<PendienteResuelto>(extraBufferCapacity = 16)

    // Lo que se va resolviendo al reenviar: las pantallas lo escuchan para mostrar el resultado cuando llega.
    val resueltas: SharedFlow<PendienteResuelto> = _resueltas.asSharedFlow()

    suspend fun enviar(respuesta: RespuestaDelEstudiante): SalidaDeEnvio = when (val resultado = repository.responder(respuesta)) {
        is ResultadoDeEnvio.Enviada -> SalidaDeEnvio.Enviada(resultado.resultado)
        ResultadoDeEnvio.SinConexion -> {
            guardar(respuesta)
            SalidaDeEnvio.Guardada
        }
        ResultadoDeEnvio.SesionVencida -> SalidaDeEnvio.SesionVencida
        else -> SalidaDeEnvio.Fallida(resultado)
    }

    // La respuesta que sigue esperando señal para este ejercicio, si hay una.
    fun pendienteDe(problemaId: Long): RespuestaDelEstudiante? = almacen.leer().firstOrNull { it.problemaId == problemaId }

    // Se llama al abrir la app y cada vez que vuelve la señal. Reenvía en orden; se detiene en la primera que no pueda
    // (sin conexión, sesión vencida o error del servidor) y deja el resto guardado. Cualquier otra contestación del
    // servidor (aceptada o rechazada) cierra esa respuesta: no tiene sentido reenviar lo que el servidor ya rechazó.
    suspend fun reenviarPendientes() {
        candado.withLock {
            for (pendiente in almacen.leer()) {
                val resultado = repository.responder(pendiente)
                if (resultado == ResultadoDeEnvio.SinConexion ||
                    resultado == ResultadoDeEnvio.SesionVencida ||
                    resultado == ResultadoDeEnvio.ErrorInesperado
                ) {
                    return
                }
                almacen.guardar(almacen.leer().filterNot { it.intentoId == pendiente.intentoId })
                _resueltas.emit(PendienteResuelto(pendiente, resultado))
            }
        }
    }

    private suspend fun guardar(respuesta: RespuestaDelEstudiante) {
        candado.withLock {
            val guardadas = almacen.leer()
            // Reintentar el mismo intento sin señal no lo duplica.
            if (guardadas.none { it.intentoId == respuesta.intentoId }) almacen.guardar(guardadas + respuesta)
        }
    }
}
