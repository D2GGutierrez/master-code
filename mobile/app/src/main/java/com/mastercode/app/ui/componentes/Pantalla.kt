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

// La cabecera de cada pantalla: una franja plana del color del fondo con una línea fina abajo, como en Consoly. Se pinta bajo la
// barra de estado y deja su alto libre.
@Composable
fun HeroMC(
    modifier: Modifier = Modifier,
    // Transparente: sin fondo ni línea, para que se vea la aurora de la pantalla (Diario, inicio de sesión).
    transparente: Boolean = false,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val fondo = if (transparente) {
        Modifier
    } else {
        Modifier
            .background(BarraOscura)
            .drawBehind {
                drawLine(BordeSobreHero, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(fondo)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 20.dp, vertical = 14.dp),
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
                    EtiquetaMC(sobretitulo, color = SobreHeroSuave)
                }
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleLarge,
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

// Una pantalla con cabecera arriba y el lienzo debajo. El cuerpo se aparta de la barra de navegación, de los bordes y del teclado,
// y avisa de que la barra de estado ya la cubrió la cabecera. Con pie (la barra de navegación inferior) el pie se aparta de la
// barra del sistema y del teclado, y el cuerpo ya no.
@Composable
fun PantallaMC(
    cabecera: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    pie: (@Composable () -> Unit)? = null,
    // Con aurora, el fondo es el de luces en movimiento (Efectos.kt) en lugar del lienzo liso.
    conAurora: Boolean = false,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val ladosDelCuerpo = if (pie == null) {
        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
    } else {
        WindowInsetsSides.Horizontal
    }
    val fondo = if (conAurora) Modifier.fondoAurora() else Modifier.background(MC.colores.lienzo)
    Column(modifier = modifier.fillMaxSize().then(fondo)) {
        cabecera()
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(ladosDelCuerpo)),
            content = contenido,
        )
        if (pie != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MC.colores.lienzo)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            ) { pie() }
        }
    }
}
