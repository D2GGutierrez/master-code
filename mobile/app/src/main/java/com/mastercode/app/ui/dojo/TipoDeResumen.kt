package com.mastercode.app.ui.dojo

import com.mastercode.app.data.dojo.ResumenDeSesion

// HU56-T2: qué mensaje principal lleva el resumen de la sesión.
enum class TipoDeResumen {
    // Terminó sin responder ningún ejercicio.
    SinRespuestas,

    // Acertó todo lo que respondió (CA-56.4): no hay temas a repasar.
    Perfecto,

    // No acertó ninguno (CA-56.5): se animan a repasar los temas.
    SinAciertos,

    Normal,
}

fun tipoDeResumen(resumen: ResumenDeSesion): TipoDeResumen = when {
    resumen.respondidos == 0 -> TipoDeResumen.SinRespuestas
    resumen.aciertos >= resumen.respondidos -> TipoDeResumen.Perfecto
    resumen.aciertos == 0 -> TipoDeResumen.SinAciertos
    else -> TipoDeResumen.Normal
}
