package com.mastercode.app.ui.componentes

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mastercode.app.ui.theme.MC

// Un bloque gris que late mientras llegan los datos: la pantalla no salta cuando aparece el contenido.
@Composable
fun Modifier.esqueleto(forma: Shape = RoundedCornerShape(6.dp)): Modifier {
    val c = MC.colores
    val transicion = rememberInfiniteTransition(label = "esqueleto")
    val opacidad by transicion.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "opacidad",
    )
    return this.alpha(opacidad).clip(forma).background(if (c.esOscuro) c.fondoSuave else c.bordeFuerte)
}

// Tarjetas de mentira mientras cargan los cursos, el perfil o las listas. La descripción es lo que oye un lector de pantalla.
@Composable
fun EsqueletoDeTarjetas(
    descripcion: String,
    modifier: Modifier = Modifier,
    cantidad: Int = 3,
    alto: Dp = 132.dp,
) {
    Column(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = descripcion },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(cantidad) {
            Box(modifier = Modifier.fillMaxWidth().height(alto).esqueleto(RoundedCornerShape(8.dp)))
        }
    }
}
