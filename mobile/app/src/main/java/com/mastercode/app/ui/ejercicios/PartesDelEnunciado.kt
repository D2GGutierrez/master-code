package com.mastercode.app.ui.ejercicios

// Reglas puras para mostrar el enunciado de los ejercicios móviles; sin Android, para probarlas solo con textos.

// La pregunta y el fragmento de código de un ejercicio «predecir la salida» (HU45-T2). codigo vacío: el enunciado no
// trae un fragmento que se pueda separar, y se muestra todo como pregunta.
data class PreguntaConCodigo(val pregunta: String, val codigo: String)

private val CERCA_DE_CODIGO = Regex("```[^\\n]*\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)

// ADR-9 D17: el fragmento va dentro del enunciado. Si está entre ``` se toma ese bloque; si no, lo que sigue a la primera
// línea en blanco (la IA escribe la pregunta, una línea en blanco y el código).
fun separarCodigo(enunciado: String): PreguntaConCodigo {
    val texto = enunciado.replace("\r\n", "\n").trim()
    CERCA_DE_CODIGO.find(texto)?.let { cerca ->
        val pregunta = texto.removeRange(cerca.range).replace(Regex("\\n{3,}"), "\n\n").trim()
        return PreguntaConCodigo(pregunta, cerca.groupValues[1].trimEnd())
    }
    val corte = texto.indexOf("\n\n")
    if (corte < 0) return PreguntaConCodigo(texto, "")
    return PreguntaConCodigo(texto.substring(0, corte).trim(), texto.substring(corte + 2).trim('\n').trimEnd())
}

// El enunciado de un ejercicio «completar el espacio» partido en el hueco (HU47-T3).
data class EnunciadoConHueco(val antes: String, val despues: String)

// El hueco es una racha de tres o más guiones bajos (ADR-9 D17); dos (__init__) no lo son. Sin hueco, null.
private val HUECO = Regex("_{3,}")

fun separarHueco(enunciado: String): EnunciadoConHueco? {
    val texto = enunciado.replace("\r\n", "\n").trimEnd()
    val hueco = HUECO.find(texto) ?: return null
    return EnunciadoConHueco(texto.substring(0, hueco.range.first), texto.substring(hueco.range.last + 1))
}
