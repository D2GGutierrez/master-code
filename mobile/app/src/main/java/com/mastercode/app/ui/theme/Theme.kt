package com.mastercode.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

private val LocalColoresMC = staticCompositionLocalOf { ColoresClaros }

// Los colores propios de MASTER CODE (los que Material 3 no tiene): MC.colores.lienzo, MC.colores.exito…
object MC {
    val colores: ColoresMC
        @Composable
        @ReadOnlyComposable
        get() = LocalColoresMC.current
}

// Radios de la web (como GitHub): 6 dp en campos y botones, 8 en tarjetas y 12 en hojas y diálogos.
val Formas = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

// Sin color dinámico: la identidad de la app (acento índigo, hero con degradado) es la misma en todos los teléfonos. Claro u oscuro
// según el sistema. Los íconos de la barra de estado los fija MainActivity (siempre claros: la barra superior es oscura).
@Composable
fun MasterCodeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val c = if (darkTheme) ColoresOscuros else ColoresClaros
    val esquema = if (darkTheme) {
        darkColorScheme(
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
    } else {
        lightColorScheme(
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
            surfaceContainerLowest = c.fondo,
            surfaceContainerLow = c.fondo,
            surfaceContainer = c.fondo,
            surfaceContainerHigh = c.fondoSuave,
            surfaceContainerHighest = c.fondoSuave,
            outline = c.bordeFuerte,
            outlineVariant = c.borde,
            error = c.error,
            onError = c.sobreAcento,
        )
    }
    CompositionLocalProvider(LocalColoresMC provides c) {
        MaterialTheme(colorScheme = esquema, typography = Tipografia, shapes = Formas, content = content)
    }
}
