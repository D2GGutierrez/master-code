package com.mastercode.app.ui.ejercicios

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Reglas puras: solo textos, sin Android.
class PartesDelEnunciadoTest {

    // ---------- predecir la salida (HU45-T2) ----------

    @Test
    fun laPreguntaVaAntesDeLaPrimeraLineaEnBlancoYElCodigoDespues() {
        val partes = separarCodigo("¿Qué imprime este código?\n\nx = 2 + 3\nprint(x)")

        assertEquals(PreguntaConCodigo("¿Qué imprime este código?", "x = 2 + 3\nprint(x)"), partes)
    }

    @Test
    fun elCodigoEntreCercasSeSeparaYSuLenguajeNoCuentaComoCodigo() {
        val partes = separarCodigo("¿Qué imprime?\n```python\nprint(1)\nprint(2)\n```\nResponde con la salida.")

        assertEquals(PreguntaConCodigo("¿Qué imprime?\n\nResponde con la salida.", "print(1)\nprint(2)"), partes)
    }

    @Test
    fun conCercasSinLenguajeTambienSeSepara() {
        val partes = separarCodigo("¿Qué imprime?\n```\nprint(1)\n```")

        assertEquals(PreguntaConCodigo("¿Qué imprime?", "print(1)"), partes)
    }

    @Test
    fun unEnunciadoSinLineaEnBlancoNiCercasSeMuestraEntero() {
        val partes = separarCodigo("¿Qué imprime print(2 + 3)?")

        assertEquals(PreguntaConCodigo("¿Qué imprime print(2 + 3)?", ""), partes)
    }

    @Test
    fun lasLineasEnBlancoDentroDelCodigoSeConservan() {
        val partes = separarCodigo("¿Qué imprime?\n\na = 1\n\nprint(a)")

        assertEquals("a = 1\n\nprint(a)", partes.codigo)
    }

    @Test
    fun losSaltosDeLineaDeWindowsSeTratanComoLosDemas() {
        val partes = separarCodigo("¿Qué imprime?\r\n\r\nprint(1)\r\nprint(2)")

        assertEquals(PreguntaConCodigo("¿Qué imprime?", "print(1)\nprint(2)"), partes)
    }

    @Test
    fun unEnunciadoVacioNoRompe() {
        assertEquals(PreguntaConCodigo("", ""), separarCodigo(""))
    }

    // ---------- completar el espacio (HU47-T3) ----------

    @Test
    fun elHuecoPartePorLaRachaDeGuionesBajos() {
        val partes = separarHueco("for i in range(3):\n    print(___)")

        assertEquals(EnunciadoConHueco("for i in range(3):\n    print(", ")"), partes)
    }

    @Test
    fun cualquierRachaDeTresOMasGuionesBajosEsElHueco() {
        assertEquals(EnunciadoConHueco("x = ", ""), separarHueco("x = ____"))
        assertEquals(EnunciadoConHueco("x = ", " + 1"), separarHueco("x = ________ + 1"))
    }

    @Test
    fun dosGuionesBajosNoSonUnHueco() {
        assertNull(separarHueco("class A:\n    def __init__(self): pass"))
    }

    @Test
    fun sinHuecoNoHayPartes() {
        assertNull(separarHueco("print(1)"))
        assertNull(separarHueco(""))
    }

    @Test
    fun elHuecoAlPrincipioOAlFinalDejaUnLadoVacio() {
        assertEquals(EnunciadoConHueco("", " = 5"), separarHueco("___ = 5"))
        assertEquals(EnunciadoConHueco("return ", ""), separarHueco("return ___\n"))
    }

    @Test
    fun conMasDeUnHuecoSoloElPrimeroEsElCampo() {
        val partes = separarHueco("a = ___\nb = ___")

        assertEquals(EnunciadoConHueco("a = ", "\nb = ___"), partes)
    }
}
