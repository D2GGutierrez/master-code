package com.mastercode.app.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mastercode.app.ui.theme.MC

// Una fila de píldoras para elegir una opción (las pestañas de Duelos, el curso del ranking). Se desliza de lado si no cabe;
// la elegida va rellena de acento y las demás con borde, así se distingue sin depender solo del color.
@Composable
fun PestanasMC(
    opciones: List<String>,
    elegida: Int,
    alElegir: (Int) -> Unit,
    modifier: Modifier = Modifier,
    rol: Role = Role.Tab,
) {
    val c = MC.colores
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        opciones.forEachIndexed { indice, texto ->
            val activa = indice == elegida
            Surface(
                shape = CircleShape,
                color = if (activa) c.acento else c.fondo,
                contentColor = if (activa) c.sobreAcento else c.texto,
                border = BorderStroke(1.dp, if (activa) c.acento else c.borde),
            ) {
                Text(
                    texto,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .selectable(selected = activa, role = rol, onClick = { alElegir(indice) })
                        .heightIn(min = 44.dp)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }
    }
}
