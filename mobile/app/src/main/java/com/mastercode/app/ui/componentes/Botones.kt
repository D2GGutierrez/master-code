package com.mastercode.app.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mastercode.app.ui.theme.MC
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer

private val FormaDeBoton = RoundedCornerShape(14.dp)

// El botón de la acción principal de la pantalla: azul sólido y grande, con el texto en mayúsculas, como en Consoly. Con cargando muestra una ruedita y no hace nada.
@Composable
fun BotonPrincipal(
    texto: String,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    icono: ImageVector? = null,
    compacto: Boolean = false,
    // Destacado: el botón respira con un halo azul y lo cruza un destello. Solo para la acción estrella de la pantalla.
    destacado: Boolean = false,
) {
    val c = MC.colores
    // Al pulsar, el botón se achica un poco y vuelve: se siente que responde al dedo.
    val fuente = remember { MutableInteractionSource() }
    val presionado by fuente.collectIsPressedAsState()
    val escala by animateFloatAsState(targetValue = if (presionado) 0.97f else 1f, animationSpec = tween(durationMillis = 90), label = "pulsar")
    Button(
        onClick = { if (!cargando) alTocar() },
        enabled = habilitado,
        interactionSource = fuente,
        modifier = modifier
            .then(if (destacado && habilitado && !cargando) Modifier.resplandor(c.primario, 14.dp).brilloQueCruza(FormaDeBoton) else Modifier)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .heightIn(min = if (compacto) 44.dp else 56.dp),
        shape = FormaDeBoton,
        colors = ButtonDefaults.buttonColors(
            containerColor = c.primario,
            contentColor = c.sobrePrimario,
            disabledContainerColor = c.borde,
            disabledContentColor = c.textoSuave,
        ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = if (compacto) 16.dp else 20.dp, vertical = if (compacto) 8.dp else 12.dp),
    ) {
        if (cargando) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
        } else {
            if (icono != null) {
                Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(texto.uppercase(), style = MaterialTheme.typography.labelLarge)
        }
    }
}

// El botón de una acción de apoyo: borde y texto normal.
@Composable
fun BotonSecundario(
    texto: String,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    icono: ImageVector? = null,
    compacto: Boolean = false,
) {
    val c = MC.colores
    OutlinedButton(
        onClick = alTocar,
        enabled = habilitado,
        modifier = modifier.heightIn(min = if (compacto) 44.dp else 56.dp),
        shape = FormaDeBoton,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = c.acento, disabledContentColor = c.textoSuave),
        border = BorderStroke(1.dp, c.acento.copy(alpha = 0.6f)),
        contentPadding = PaddingValues(horizontal = if (compacto) 16.dp else 20.dp, vertical = if (compacto) 8.dp else 12.dp),
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(texto.uppercase(), style = MaterialTheme.typography.labelLarge)
    }
}

// Una acción discreta (volver, actualizar…): solo texto, con 48 dp de alto.
@Composable
fun BotonDeTexto(
    texto: String,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    TextButton(
        onClick = alTocar,
        enabled = habilitado,
        modifier = modifier.heightIn(min = 48.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = MC.colores.acento, disabledContentColor = MC.colores.textoSuave),
    ) { Text(texto.uppercase(), style = MaterialTheme.typography.labelLarge) }
}
