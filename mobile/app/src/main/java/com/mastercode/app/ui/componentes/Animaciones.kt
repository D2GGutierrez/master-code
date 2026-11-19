package com.mastercode.app.ui.componentes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

// Los movimientos que dan vida a la app: entrada de tarjetas, rebote del veredicto, contadores que suben y confeti. Todos son
// cortos y solo animan al aparecer; ninguno bloquea el toque ni cambia lo que dice el lector de pantalla.

// La pieza aparece subiendo unos píxeles y ganando opacidad. Con retrasoMs se escalonan varias piezas seguidas (0, 70, 140…).
@Composable
fun Modifier.entrada(retrasoMs: Int = 0): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (retrasoMs > 0) delay(retrasoMs.toLong())
        visible = true
    }
    val progreso by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "entrada",
    )
    return this.graphicsLayer {
        alpha = progreso
        translationY = (1f - progreso) * 28.dp.toPx()
    }
}

// La pieza aparece creciendo con un pequeño rebote: para el veredicto («Resuelto» o «Incorrecto»).
@Composable
fun Modifier.aparecerConRebote(): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val escala by animateFloatAsState(
        targetValue = if (visible) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "rebote",
    )
    val opacidad by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "reboteOpacidad",
    )
    return this.graphicsLayer {
        scaleX = escala
        scaleY = escala
        alpha = opacidad
    }
}

// Un número que sube de 0 hasta valor al aparecer (puntos, aciertos). Si valor cambia, sube o baja hasta el nuevo.
@Composable
fun contadorAnimado(valor: Int, duracionMs: Int = 1000): Int {
    var objetivo by remember { mutableIntStateOf(0) }
    LaunchedEffect(valor) { objetivo = valor }
    val actual by animateIntAsState(
        targetValue = objetivo,
        animationSpec = tween(durationMillis = duracionMs, easing = FastOutSlowInEasing),
        label = "contador",
    )
    return actual
}

private val COLORES_DE_CONFETI = listOf(
    Color(0xFF2B90E8),
    Color(0xFFF5A524),
    Color(0xFF4CC15F),
    Color(0xFFFF8A3D),
    Color(0xFFB36BFF),
    Color(0xFFF85149),
)

// Confeti que cae una sola vez sobre lo que tenga debajo (se pone encima en un Box). No recibe toques. Cada pieza tiene su posición,
// velocidad y giro, fijos por número de pieza, así que se ve igual si la pantalla se recompone.
@Composable
fun ConfetiMC(modifier: Modifier = Modifier, cantidad: Int = 64, duracionMs: Int = 2800) {
    val progreso = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progreso.animateTo(1f, animationSpec = tween(durationMillis = duracionMs, easing = LinearEasing))
    }
    Canvas(modifier = modifier) {
        val avance = progreso.value
        for (i in 0 until cantidad) {
            val azar = Random(i * 7919 + 13)
            val inicioX = azar.nextFloat() * size.width
            val deriva = (azar.nextFloat() - 0.5f) * size.width * 0.25f
            val velocidad = 0.75f + azar.nextFloat() * 0.6f
            val retraso = azar.nextFloat() * 0.25f
            val t = ((avance - retraso) / (1f - retraso)).coerceIn(0f, 1f)
            if (t <= 0f) continue
            val x = inicioX + deriva * sin(t * 6f + i)
            val y = -24.dp.toPx() + t * velocidad * (size.height + 48.dp.toPx())
            val lado = (5 + azar.nextInt(5)).dp.toPx()
            val giro = t * (360f + azar.nextFloat() * 720f)
            val opacidad = if (t > 0.8f) (1f - t) / 0.2f else 1f
            val color = COLORES_DE_CONFETI[i % COLORES_DE_CONFETI.size]
            rotate(giro, pivot = Offset(x, y)) {
                drawRect(
                    color = color.copy(alpha = opacidad),
                    topLeft = Offset(x - lado / 2f, y - lado / 2f),
                    size = Size(lado, lado * 0.6f),
                )
            }
        }
    }
}
