package com.mastercode.app.ui.componentes

import java.util.Locale

// «00:16»: minutos y segundos con dos cifras. Pura, para probarla sin Android. Un tiempo negativo cuenta como cero.
fun formatoDeTiempo(segundos: Int): String {
    val total = if (segundos < 0) 0 else segundos
    return String.format(Locale.ROOT, "%02d:%02d", total / 60, total % 60)
}
