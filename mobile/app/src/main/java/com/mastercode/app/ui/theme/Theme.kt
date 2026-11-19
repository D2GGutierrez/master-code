package com.mastercode.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

private val LocalColoresMC = staticCompositionLocalOf { ColoresOscuros }

// Los colores propios de MASTER CODE (los que Material 3 no tiene): MC.colores.lienzo, MC.colores.exito…
object MC {
    val colores: ColoresMC
        @Composable
        @ReadOnlyComposable
        get() = LocalColoresMC.current
}

// Radios estilo Consoly: campos y chips de 12 dp, tarjetas de 16 y hojas y diálogos de 20.
val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

// Sin color dinámico: la identidad de la app (fondo azul marino y acento azul) es la misma en todos los teléfonos. La app es
// oscura siempre, como Consoly; el parámetro darkTheme se conserva por compatibilidad y ya no cambia nada.
@Suppress("UNUSED_PARAMETER")
@Composable
fun MasterCodeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val c = ColoresOscuros
    val esquema = darkColorScheme(
        primary = c.acento,
        onPrimary = c.sobreAcento,
        primaryContainer = c.acentoSuave,
        onPrimaryContainer = c.acento,
        secondary = c.acento,
        onSecondary = c.sobreAcento,
        background = c.lienzo,
        onBackground = c.texto,
        surface = c.fondo,
        onSurface = c.texto,
        surfaceVariant = c.fondoSuave,
        onSurfaceVariant = c.textoSuave,
        surfaceContainerLowest = c.lienzo,
        surfaceContainerLow = c.fondo,
        surfaceContainer = c.fondo,
        surfaceContainerHigh = c.fondoSuave,
        surfaceContainerHighest = c.fondoSuave,
        outline = c.bordeFuerte,
        outlineVariant = c.borde,
        error = c.error,
        onError = c.sobreAcento,
    )
    CompositionLocalProvider(LocalColoresMC provides c) {
        MaterialTheme(colorScheme = esquema, typography = Tipografia, shapes = Formas, content = content)
    }
}
