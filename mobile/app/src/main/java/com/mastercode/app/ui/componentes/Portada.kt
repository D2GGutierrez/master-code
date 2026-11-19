package com.mastercode.app.ui.componentes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mastercode.app.R
import com.mastercode.app.ui.theme.MC
import com.mastercode.app.ui.theme.paletaDe

// La ficha de un lenguaje para elegir (Dojo): una caja con borde fino, el logo del lenguaje, el nombre y una flecha.
// Toda ella se toca.
@Composable
fun FichaDeLenguaje(lenguaje: String, nombre: String, alTocar: () -> Unit, modifier: Modifier = Modifier) {
    TarjetaMC(modifier = modifier.fillMaxWidth(), alTocar = alTocar) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LogoLenguaje(lenguaje, 28.dp)
            Text(nombre, style = MaterialTheme.typography.titleMedium, color = MC.colores.acento, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MC.colores.textoSuave, modifier = Modifier.size(24.dp))
        }
    }
}

// El logo del lenguaje (Python, Java, JavaScript) para ponerlo junto a su nombre. Es decorativo: el nombre ya va escrito al lado,
// así que no tiene descripción. Si el lenguaje no tiene logo se queda el punto de color de siempre.
@Composable
fun LogoLenguaje(lenguaje: String, tamano: Dp, modifier: Modifier = Modifier) {
    val logo = when (lenguaje.trim().lowercase()) {
        "python" -> R.drawable.leng_python
        "java" -> R.drawable.leng_java
        "javascript" -> R.drawable.leng_javascript
        else -> null
    }
    if (logo != null) {
        Image(painter = painterResource(logo), contentDescription = null, modifier = modifier.size(tamano))
    } else {
        Box(modifier = modifier.size(tamano), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(tamano / 2).background(paletaDe(lenguaje).p1, CircleShape))
        }
    }
}
