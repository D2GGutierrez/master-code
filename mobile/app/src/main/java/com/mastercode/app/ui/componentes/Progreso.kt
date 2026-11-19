package com.mastercode.app.ui.componentes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mastercode.app.ui.theme.MC

// Una barra de avance con la pista del tema y el relleno en degradado de acento. Se llena con una animación corta.
@Composable
fun BarraDeProgreso(
    progreso: Float,
    modifier: Modifier = Modifier,
    descripcion: String? = null,
    alto: Dp = 8.dp,
    colorDeLaPista: Color = MC.colores.borde,
    colorDelRelleno: Color = MC.colores.acento,
) {
    val valor by animateFloatAsState(targetValue = progreso.coerceIn(0f, 1f), animationSpec = tween(durationMillis = 700), label = "progreso")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(alto)
            .clip(CircleShape)
            .background(colorDeLaPista)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(valor, 0f..1f)
                if (descripcion != null) contentDescription = descripcion
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(valor)
                .clip(CircleShape)
                .background(colorDelRelleno),
        )
    }
}

// El anillo de avance (por ejemplo, aciertos de respondidos) con su contenido al centro.
@Composable
fun AnilloDeProgreso(
    progreso: Float,
    modifier: Modifier = Modifier,
    tamano: Dp = 132.dp,
    grosor: Dp = 12.dp,
    colorDeLaPista: Color = MC.colores.borde,
    colorDelArco: Color = MC.colores.acento,
    contenido: @Composable BoxScope.() -> Unit = {},
) {
    val valor by animateFloatAsState(targetValue = progreso.coerceIn(0f, 1f), animationSpec = tween(durationMillis = 900), label = "anillo")
    Box(modifier = modifier.size(tamano), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(tamano)) {
            val trazo = grosor.toPx()
            val lado = size.minDimension - trazo
            val esquina = Offset(trazo / 2f, trazo / 2f)
            drawArc(
                color = colorDeLaPista,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = esquina,
                size = Size(lado, lado),
                style = Stroke(width = trazo, cap = StrokeCap.Round),
            )
            if (valor > 0f) {
                drawArc(
                    color = colorDelArco,
                    startAngle = -90f,
                    sweepAngle = 360f * valor,
                    useCenter = false,
                    topLeft = esquina,
                    size = Size(lado, lado),
                    style = Stroke(width = trazo, cap = StrokeCap.Round),
                )
            }
        }
        contenido()
    }
}
