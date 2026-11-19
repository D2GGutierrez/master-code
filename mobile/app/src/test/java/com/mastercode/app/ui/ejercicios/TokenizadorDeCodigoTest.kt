package com.mastercode.app.ui.ejercicios

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenizadorDeCodigoTest {
    private fun tiposDe(linea: String, tipo: TipoDeToken) = tokenizarLinea(linea).filter { it.tipo == tipo }.map { it.texto }

    @Test
    fun losTrozosJuntosDanLaLineaOriginal() {
        val linea = "for i in range(3):  # cuenta"
        assertEquals(linea, tokenizarLinea(linea).joinToString("") { it.texto })
    }

    @Test
    fun unaLineaVaciaNoTieneTrozos() {
        assertTrue(tokenizarLinea("").isEmpty())
    }

    @Test
    fun reconocePalabrasReservadas() {
        assertEquals(listOf("def", "return"), tiposDe("def f(x): return x", TipoDeToken.Palabra))
    }

    @Test
    fun reconocePalabrasDeJavaYJavaScript() {
        assertEquals(listOf("public", "static", "void"), tiposDe("public static void main", TipoDeToken.Palabra))
        assertEquals(listOf("const"), tiposDe("const total = 1", TipoDeToken.Palabra))
    }

    @Test
    fun unNombreSeguidoDeParentesisEsUnaLlamada() {
        assertEquals(listOf("suma"), tiposDe("suma(1, 2)", TipoDeToken.Llamada))
    }

    @Test
    fun unaCadenaLlegaHastaSuComillaDeCierre() {
        assertEquals(listOf("\"hola\""), tiposDe("x = \"hola\" + y", TipoDeToken.Texto))
        assertEquals(listOf("'a\\'b'"), tiposDe("'a\\'b' + 1", TipoDeToken.Texto))
    }

    @Test
    fun unaCadenaSinCerrarLlegaHastaElFinalDeLaLinea() {
        assertEquals(listOf("\"abierta"), tiposDe("\"abierta", TipoDeToken.Texto))
    }

    @Test
    fun reconocePalabrasEnLaCadenaSinPintarlasComoPalabras() {
        val tokens = tokenizarLinea("\"for if\"")
        assertEquals(1, tokens.size)
        assertEquals(TipoDeToken.Texto, tokens[0].tipo)
    }

    @Test
    fun losNumerosPuedenTenerDecimales() {
        assertEquals(listOf("3.14", "10"), tiposDe("a = 3.14 * 10", TipoDeToken.Numero))
    }

    @Test
    fun elComentarioLlegaHastaElFinalConHashOConDobleBarra() {
        assertEquals(listOf("# nota"), tiposDe("x = 1  # nota", TipoDeToken.Comentario))
        assertEquals(listOf("// nota con \"comillas\""), tiposDe("int x = 1; // nota con \"comillas\"", TipoDeToken.Comentario))
    }

    @Test
    fun loQueSigueAlComentarioNoSePintaDeOtraCosa() {
        val tipos = tokenizarLinea("# def return 5").map { it.tipo }
        assertEquals(listOf(TipoDeToken.Comentario), tipos)
    }

    @Test
    fun losSignosSonSignos() {
        assertEquals(listOf("=", "+"), tiposDe("a = b + c", TipoDeToken.Signo))
    }
}
