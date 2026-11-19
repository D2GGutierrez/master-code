package com.mastercode.app.ui.ejercicios

// Reglas puras para pintar el código con colores, sin Android: parten una línea en trozos y dicen de qué tipo es cada uno. Sirve
// para Python, Java y JavaScript a la vez (las palabras reservadas de los tres juntas); no es un análisis del lenguaje, solo un
// resaltado que ayuda a leer. Cada línea se mira sola: una cadena o un comentario de varias líneas no se sigue de una a otra.

enum class TipoDeToken { Normal, Palabra, Texto, Numero, Comentario, Llamada, Signo }

data class Token(val texto: String, val tipo: TipoDeToken)

private val PALABRAS_RESERVADAS = setOf(
    // Python
    "def", "class", "return", "if", "elif", "else", "for", "while", "in", "not", "and", "or", "is", "import", "from", "as", "pass",
    "break", "continue", "None", "True", "False", "lambda", "try", "except", "finally", "with", "yield", "print", "range",
    // Java
    "public", "private", "protected", "static", "final", "void", "int", "long", "double", "float", "boolean", "char", "String",
    "new", "this", "extends", "implements", "interface", "package", "throw", "throws", "switch", "case", "default", "do", "null",
    "System",
    // JavaScript
    "const", "let", "var", "function", "typeof", "undefined", "true", "false", "of", "async", "await", "export", "console",
)

private const val SIGNOS = "()[]{}<>=+-*/%!&|:;,.?"

fun tokenizarLinea(linea: String): List<Token> {
    val tokens = mutableListOf<Token>()
    val n = linea.length
    var i = 0
    while (i < n) {
        val c = linea[i]
        var j = i + 1
        val tipo: TipoDeToken
        if (c == '#' || linea.startsWith("//", i)) {
            // Un comentario llega hasta el final de la línea.
            j = n
            tipo = TipoDeToken.Comentario
        } else if (c == '"' || c == '\'' || c == '`') {
            // Una cadena llega hasta la comilla que la cierra (o hasta el final de la línea si no se cierra).
            while (j < n && linea[j] != c) {
                if (linea[j] == '\\' && j + 1 < n) j++
                j++
            }
            if (j < n) j++
            tipo = TipoDeToken.Texto
        } else if (c.isDigit()) {
            while (j < n && (linea[j].isLetterOrDigit() || linea[j] == '.')) j++
            tipo = TipoDeToken.Numero
        } else if (c.isLetter() || c == '_') {
            while (j < n && (linea[j].isLetterOrDigit() || linea[j] == '_')) j++
            val palabra = linea.substring(i, j)
            tipo = when {
                palabra in PALABRAS_RESERVADAS -> TipoDeToken.Palabra
                j < n && linea[j] == '(' -> TipoDeToken.Llamada
                else -> TipoDeToken.Normal
            }
        } else if (SIGNOS.indexOf(c) >= 0) {
            tipo = TipoDeToken.Signo
        } else {
            tipo = TipoDeToken.Normal
        }
        tokens.add(Token(linea.substring(i, j), tipo))
        i = j
    }
    return tokens
}
