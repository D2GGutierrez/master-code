package com.mastercode.app.ui.componentes

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mastercode.app.ui.theme.AuroraAzul
import com.mastercode.app.ui.theme.AuroraVioleta
import com.mastercode.app.ui.theme.MC
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap

// Los efectos de la app (fondo, anillos que giran, brillos, resplandores, texto que se escribe). El fondo es quieto; los demás son
// piezas chicas que se mueven. Leen el valor animado
// solo al dibujar (drawBehind, drawWithContent, graphicsLayer), así que cada cuadro se repinta sin recomponer la pantalla. Si en el
// teléfono están desactivadas las animaciones (Opciones de desarrollador o Accesibilidad → quitar animaciones), todos quedan quietos.

// ¿El teléfono permite animaciones? Con la escala de animación en 0 los efectos se dibujan quietos.
@Composable
fun animacionesActivas(): Boolean {
    val contexto = LocalContext.current
    return remember(contexto) {
        Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}

// El fondo de las pantallas principales: el lienzo casi negro con dos luces muy suaves y quietas (azul arriba a la izquierda, violeta a
// la derecha) y una grilla de puntos tenue, como el fondo de un editor. No se mueve: un fondo en movimiento detrás del texto marea.
@Composable
fun Modifier.fondoAurora(): Modifier {
    val lienzo = MC.colores.lienzo
    return this.drawWithCache {
        val w = size.width
        val h = size.height
        val radio = max(w, h) * 0.6f
        val luzAzul = Offset(w * 0.1f, h * 0.05f)
        val luzVioleta = Offset(w * 1.0f, h * 0.55f)
        val brilloAzul = Brush.radialGradient(listOf(AuroraAzul.copy(alpha = 0.16f), Color.Transparent), center = luzAzul, radius = radio)
        val brilloVioleta = Brush.radialGradient(listOf(AuroraVioleta.copy(alpha = 0.10f), Color.Transparent), center = luzVioleta, radius = radio * 0.85f)
        val paso = 24.dp.toPx()
        val puntos = ArrayList<Offset>()
        var y = paso / 2f
        while (y < h) {
            var x = paso / 2f
            while (x < w) {
                puntos.add(Offset(x, y))
                x += paso
            }
            y += paso
        }
        val grosorDelPunto = 1.5.dp.toPx()
        onDrawBehind {
            drawRect(lienzo)
            drawCircle(brush = brilloAzul, radius = radio, center = luzAzul)
            drawCircle(brush = brilloVioleta, radius = radio * 0.85f, center = luzVioleta)
            drawPoints(puntos, PointMode.Points, Color.White.copy(alpha = 0.035f), strokeWidth = grosorDelPunto, cap = StrokeCap.Round)
        }
    }
}

// Un halo de luz alrededor de la pieza, que respira (crece y se apaga) cada 1,7 segundos. radio es el de las esquinas de la pieza; para
// una pieza redonda, la mitad de su lado. Se dibuja por fuera de la pieza, así que conviene dejarle margen alrededor.
@Composable
fun Modifier.resplandor(color: Color, radio: Dp, intensidad: Float = 1f, pulsar: Boolean = true): Modifier {
    val activas = animacionesActivas() && pulsar
    val pulso = rememberInfiniteTransition(label = "resplandor").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1700, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "pulsoDeResplandor",
    )
    return this.drawBehind {
        val k = if (activas) pulso.value else 0.6f
        val base = radio.toPx()
        for (capa in 1..5) {
            val crecer = capa * 3.dp.toPx() * (0.7f + 0.5f * k)
            val alfa = (intensidad * 0.09f * (6 - capa) / 5f * (0.55f + 0.45f * k)).coerceIn(0f, 1f)
            drawRoundRect(
                color = color.copy(alpha = alfa),
                topLeft = Offset(-crecer, -crecer),
                size = Size(size.width + 2f * crecer, size.height + 2f * crecer),
                cornerRadius = CornerRadius(base + crecer),
            )
        }
    }
}

// Un destello diagonal que cruza la pieza de lado a lado y descansa un momento antes de volver a pasar (botón de práctica, tarjeta dorada).
@Composable
fun Modifier.brilloQueCruza(forma: Shape, intensidad: Float = 0.28f, periodoMs: Int = 2800): Modifier {
    val activas = animacionesActivas()
    val avance = rememberInfiniteTransition(label = "brillo").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = periodoMs, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "avanceDelBrillo",
    )
    return this.clip(forma).drawWithContent {
        drawContent()
        if (!activas) return@drawWithContent
        // El destello ocupa el 55 % de la vuelta; el resto del tiempo la pieza queda quieta.
        val p = (avance.value / 0.55f).coerceAtMost(1f)
        if (p >= 1f) return@drawWithContent
        val banda = size.width * 0.35f
        val x = -banda + p * (size.width + 2f * banda)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = intensidad), Color.Transparent),
                start = Offset(x - banda, 0f),
                end = Offset(x + banda, size.height),
            ),
        )
    }
}

// Un anillo de luz que gira alrededor de una pieza redonda: un degradado circular de los colores dados, con un tramo transparente para
// que se note el giro.
@Composable
fun Modifier.anilloGiratorio(colores: List<Color>, grosor: Dp = 2.dp, periodoMs: Int = 3200): Modifier {
    val activas = animacionesActivas()
    val giro = rememberInfiniteTransition(label = "anillo").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = periodoMs, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "giroDelAnillo",
    )
    return this.drawWithContent {
        drawContent()
        val trazo = grosor.toPx()
        rotate(if (activas) giro.value else 0f) {
            drawCircle(
                brush = Brush.sweepGradient(colores, center),
                radius = size.minDimension / 2f - trazo / 2f,
                center = center,
                style = Stroke(width = trazo),
            )
        }
    }
}

// La pieza sube y baja unos píxeles, despacio, como si flotara (el logo del inicio de sesión).
@Composable
fun Modifier.flotar(amplitud: Dp = 5.dp, periodoMs: Int = 3600): Modifier {
    val activas = animacionesActivas()
    val fase = rememberInfiniteTransition(label = "flotar").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = periodoMs, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "faseDeFlotar",
    )
    return this.graphicsLayer {
        translationY = if (activas) sin(fase.value * 2f * PI.toFloat()) * amplitud.toPx() else 0f
    }
}

// Un texto que se escribe letra por letra, como en una terminal, con un cursor que parpadea al final. El resto del texto ya ocupa su
// lugar (transparente), así que nada salta mientras se escribe. El lector de pantalla lee el texto completo desde el principio.
@Composable
fun TextoQueSeEscribe(
    texto: String,
    estilo: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = MC.colores.texto,
    alineacion: TextAlign? = null,
    retrasoMs: Int = 0,
    conCursor: Boolean = true,
    encabezado: Boolean = false,
    // Un prompt de terminal antes del texto («> », «$ »), en el color del acento. No se escribe: está desde el principio.
    prefijo: String = "",
) {
    val activas = animacionesActivas()
    var visibles by remember(texto) { mutableIntStateOf(if (activas) 0 else texto.length) }
    var cursorEncendido by remember { mutableStateOf(true) }
    LaunchedEffect(texto, activas) {
        if (!activas) {
            visibles = texto.length
            return@LaunchedEffect
        }
        if (retrasoMs > 0) delay(retrasoMs.toLong())
        while (visibles < texto.length) {
            delay(45)
            visibles++
        }
    }
    if (conCursor && activas) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(530)
                cursorEncendido = !cursorEncendido
            }
        }
    }
    val acento = MC.colores.acento
    Text(
        buildAnnotatedString {
            if (prefijo.isNotEmpty()) {
                withStyle(SpanStyle(color = acento)) { append(prefijo) }
            }
            append(texto.take(visibles))
            if (conCursor) {
                withStyle(SpanStyle(color = if (cursorEncendido) acento else Color.Transparent)) { append("▌") }
            }
            withStyle(SpanStyle(color = Color.Transparent)) { append(texto.drop(visibles)) }
        },
        style = estilo,
        color = color,
        textAlign = alineacion,
        modifier = modifier.clearAndSetSemantics {
            contentDescription = texto
            if (encabezado) heading()
        },
    )
}
