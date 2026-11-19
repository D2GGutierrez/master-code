package com.mastercode.app.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mastercode.app.R
import com.mastercode.app.ui.theme.Barra
import com.mastercode.app.ui.theme.BarraOscura
import com.mastercode.app.ui.theme.BordeSobreHero
import com.mastercode.app.ui.theme.CristalSobreHero
import com.mastercode.app.ui.theme.MC
import com.mastercode.app.ui.theme.SobreHero
import com.mastercode.app.ui.theme.SobreHeroSuave

// El «hero»: la barra oscura y plana que abre cada pantalla, como la cabecera de GitHub: un solo color y una línea fina abajo.
// Es oscura en los dos temas, así que los íconos de la barra de estado son siempre claros. Se pinta bajo la barra de estado y
// deja su alto libre.
@Composable
fun HeroMC(
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val fondo = if (MC.colores.esOscuro) BarraOscura else Barra
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(fondo)
            .drawBehind {
                drawLine(BordeSobreHero, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            }
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        content = contenido,
    )
}

// Un hero con título (y botón para volver): lo que encabeza Perfil, Avisos, Duelos, Dojo y la partida.
@Composable
fun CabeceraMC(
    titulo: String,
    modifier: Modifier = Modifier,
    sobretitulo: String? = null,
    alVolver: (() -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {},
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    HeroMC(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (alVolver != null) BotonDeIconoSobreHero(alVolver)
            Column(modifier = Modifier.weight(1f)) {
                if (sobretitulo != null) {
                    Text(sobretitulo.uppercase(), style = MaterialTheme.typography.labelSmall, color = SobreHeroSuave)
                }
                Text(
                    titulo,
                    style = MaterialTheme.typography.headlineMedium,
                    color = SobreHero,
                    modifier = Modifier.semantics { heading() },
                )
            }
            acciones()
        }
        extra()
    }
}

// El botón redondo de «volver», en vidrio sobre el hero. Mide 44 dp para tocarse sin errar.
@Composable
fun BotonDeIconoSobreHero(alTocar: () -> Unit, modifier: Modifier = Modifier) {
    val descripcion = stringResource(R.string.accion_volver)
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(CristalSobreHero)
            .border(BorderStroke(1.dp, BordeSobreHero), CircleShape)
            .clickable(onClickLabel = descripcion, role = Role.Button, onClick = alTocar),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = descripcion, tint = SobreHero, modifier = Modifier.size(22.dp))
    }
}

// Una acción de texto sobre el hero («Salir», «Terminar sesión»): letra clara, 48 dp de alto.
@Composable
fun BotonDeTextoSobreHero(texto: String, alTocar: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = alTocar,
        modifier = modifier.heightIn(min = 48.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = SobreHero),
    ) { Text(texto, style = MaterialTheme.typography.labelLarge) }
}

// Una pantalla con cabecera oscura arriba y el lienzo del tema debajo. El cuerpo se aparta de la barra de navegación, de los
// bordes y del teclado, y avisa de que la barra de estado ya la cubrió la cabecera.
@Composable
fun PantallaMC(
    cabecera: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().background(MC.colores.lienzo)) {
        cabecera()
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            content = contenido,
        )
    }
}
