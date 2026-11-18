package com.mastercode.app.ui.cinturon

import androidx.annotation.StringRes
import com.mastercode.app.R

// El nombre del cinturón con su tilde («marrón»), por nivel: de 1 (blanco) a 7 (negro), como en docs/puntos-cinturon.md.
// null si el nivel no se conoce: entonces se usa el nombre que mande user-api.
@StringRes
fun nombreDeCinturon(nivel: Int): Int? = when (nivel) {
    1 -> R.string.cinturon_blanco
    2 -> R.string.cinturon_amarillo
    3 -> R.string.cinturon_naranja
    4 -> R.string.cinturon_verde
    5 -> R.string.cinturon_azul
    6 -> R.string.cinturon_marron
    7 -> R.string.cinturon_negro
    else -> null
}
