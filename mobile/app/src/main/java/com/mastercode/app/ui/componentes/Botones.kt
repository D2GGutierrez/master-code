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

private val FormaDeBoton = RoundedCornerShape(6.dp)

// El botón de la acción principal de la pantalla: verde sólido como el de GitHub, 52 dp de alto. Con cargando muestra una ruedita y no hace nada.
@Composable
fun BotonPrincipal(
    texto: String,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    icono: ImageVector? = null,
    compacto: Boolean = false,
) {
    val c = MC.colores
    Button(
        onClick = { if (!cargando) alTocar() },
        enabled = habilitado,
        modifier = modifier.heightIn(min = if (compacto) 44.dp else 52.dp),
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
            Text(texto, style = MaterialTheme.typography.labelLarge)
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
        modifier = modifier.heightIn(min = if (compacto) 44.dp else 52.dp),
        shape = FormaDeBoton,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = c.texto, disabledContentColor = c.textoSuave),
        border = BorderStroke(1.dp, c.bordeFuerte),
        contentPadding = PaddingValues(horizontal = if (compacto) 16.dp else 20.dp, vertical = if (compacto) 8.dp else 12.dp),
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelLarge)
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
    ) { Text(texto, style = MaterialTheme.typography.labelLarge) }
}
