package com.mastercode.app.ui.duelos

import androidx.annotation.StringRes
import com.mastercode.app.R
import com.mastercode.app.data.duelos.Marcador
import com.mastercode.app.data.duelos.PartidaDelHistorial
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// Los textos de las pantallas de Duelos que dependen de lo que contesta user-api, sin Compose para poder probarlos.

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

// La fecha del historial como dd/MM/yyyy en la zona del teléfono; si no se puede leer, los 10 primeros caracteres.
internal fun fechaCorta(iso: String?, zona: ZoneId = ZoneId.systemDefault()): String? = iso?.let {
    try {
        OffsetDateTime.parse(it).atZoneSameInstant(zona).toLocalDate().format(FORMATO_FECHA)
    } catch (e: java.time.format.DateTimeParseException) {
        it.take(10)
    }
}

// El estado de una partida del historial: quién ganó si terminó, o cómo está.
@StringRes
internal fun etiquetaDeLaPartida(partida: PartidaDelHistorial): Int = when (partida.estado) {
    "terminada" -> when (partida.resultado) {
        "gane" -> R.string.historial_gane
        "perdi" -> R.string.historial_perdi
        else -> R.string.historial_empate
    }
    "en_curso" -> R.string.historial_en_curso
    "pendiente" -> R.string.historial_pendiente
    "rechazada" -> R.string.historial_rechazada
    else -> R.string.historial_caducada
}

// Solo una partida en curso se puede retomar desde el historial.
internal fun sePuedeContinuar(partida: PartidaDelHistorial): Boolean = partida.estado == "en_curso"

internal fun porcentajeTexto(porcentaje: Double): String = "${porcentaje.roundToInt()} %"

// El título del final de la partida y, si terminó por abandono, la frase que lo explica.
internal class TextoFinal(@param:StringRes val titulo: Int, @param:StringRes val motivo: Int?)

internal fun textoFinalDe(marcador: Marcador): TextoFinal {
    val abandono = marcador.motivoFin == "abandono"
    return when (marcador.resultado) {
        "gane" -> TextoFinal(R.string.partida_gane, if (abandono) R.string.partida_final_abandono_gane else null)
        "perdi" -> TextoFinal(R.string.partida_perdi, if (abandono) R.string.partida_final_abandono_perdi else null)
        else -> TextoFinal(R.string.partida_empate, null)
    }
}
