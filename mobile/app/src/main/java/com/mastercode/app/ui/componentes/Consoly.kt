package com.mastercode.app.ui.componentes

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mastercode.app.R
import com.mastercode.app.ui.theme.BannerAzul
import com.mastercode.app.ui.theme.BannerVioleta
import com.mastercode.app.ui.theme.Dorado
import com.mastercode.app.ui.theme.DoradoFondoA
import com.mastercode.app.ui.theme.DoradoFondoB
import com.mastercode.app.ui.theme.EstiloDeCodigo
import com.mastercode.app.ui.theme.FondoDeBatalla
import com.mastercode.app.ui.theme.FuegoDeRacha
import com.mastercode.app.ui.theme.MC
import com.mastercode.app.ui.theme.NaranjaDeBatalla
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.mastercode.app.ui.theme.CodigoComentario
import com.mastercode.app.ui.theme.VentanaAmarillo
import com.mastercode.app.ui.theme.VentanaRojo
import com.mastercode.app.ui.theme.VentanaVerde

// Las piezas con las que la app móvil copia el estilo de Consoly: etiquetas en mayúsculas con letra monoespaciada, la píldora del
// tiempo, los chips, la barra de navegación con el botón de Batallas al centro, la tarjeta dorada de puntos y el banner violeta.

// Una etiqueta como «// META DE HOY» o «// DESGLOSE DE PUNTOS»: escrita como un comentario de código, con las dos barras en el gris de
// los comentarios, y el texto en mayúsculas con letra monoespaciada. El lector de pantalla lee solo el texto, sin las barras.
@Composable
fun EtiquetaMC(
    texto: String,
    modifier: Modifier = Modifier,
    color: Color = MC.colores.textoSuave,
    comentario: Boolean = true,
) {
    val enMayusculas = texto.uppercase()
    Text(
        buildAnnotatedString {
            if (comentario) {
                withStyle(SpanStyle(color = CodigoComentario)) { append("// ") }
            }
            append(enMayusculas)
        },
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier.clearAndSetSemantics { contentDescription = texto },
    )
}

// Una tarjeta con forma de ventana de editor: una barra arriba con los tres puntos de colores y el nombre del archivo en letra de
// código, y el contenido debajo. El nombre del archivo es decoración (el lector de pantalla no lo lee).
@Composable
fun VentanaMC(
    archivo: String,
    modifier: Modifier = Modifier,
    alTocar: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val c = MC.colores
    TarjetaMC(modifier = modifier, alTocar = alTocar) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.fondoSuave)
                .padding(horizontal = 14.dp, vertical = 9.dp)
                .clearAndSetSemantics {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (punto in listOf(VentanaRojo, VentanaAmarillo, VentanaVerde)) {
                Box(modifier = Modifier.size(10.dp).background(punto, CircleShape))
            }
            Text(
                archivo,
                style = EstiloDeCodigo.copy(fontSize = 12.sp, lineHeight = 16.sp),
                color = c.textoSuave,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(c.borde))
        contenido()
    }
}

// Una píldora con borde fino (el tiempo, las metas): fondo de tarjeta y esquinas del todo redondas.
@Composable
fun PildoraMC(modifier: Modifier = Modifier, contenido: @Composable RowScope.() -> Unit) {
    val c = MC.colores
    Row(
        modifier = modifier
            .background(c.fondo, CircleShape)
            .border(1.dp, c.borde, CircleShape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = contenido,
    )
}

// El tiempo que lleva el ejercicio: reloj verde y cifras grandes en letra monoespaciada.
@Composable
fun TemporizadorMC(segundos: Int, modifier: Modifier = Modifier) {
    val c = MC.colores
    val texto = formatoDeTiempo(segundos)
    val descripcion = stringResource(R.string.tiempo_descripcion, texto)
    PildoraMC(modifier = modifier.clearAndSetSemantics { contentDescription = descripcion }) {
        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = c.exito, modifier = Modifier.size(22.dp))
        Text(texto, style = EstiloDeCodigo.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold), color = c.texto)
    }
}

// Un chip con borde de color y fondo tenue («PREDECIR LA SALIDA»).
@Composable
fun ChipMC(texto: String, modifier: Modifier = Modifier, color: Color = MC.colores.acento) {
    val forma = RoundedCornerShape(10.dp)
    Text(
        texto.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
            .background(color.copy(alpha = 0.10f), forma)
            .border(1.dp, color.copy(alpha = 0.45f), forma)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

// «INTENTOS RESTANTES ●●●»: un punto relleno por cada intento que queda y uno vacío por cada uno que se gastó.
@Composable
fun IntentosMC(restantes: Int, modifier: Modifier = Modifier, total: Int = 3) {
    val c = MC.colores
    val forma = RoundedCornerShape(10.dp)
    val puntos = if (total > restantes) total else restantes
    val descripcion = stringResource(R.string.intentos_descripcion, restantes)
    Row(
        modifier = modifier
            .background(c.acento.copy(alpha = 0.10f), forma)
            .border(1.dp, c.acento.copy(alpha = 0.45f), forma)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clearAndSetSemantics { contentDescription = descripcion },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.intentos_etiqueta).uppercase(), style = MaterialTheme.typography.labelMedium, color = c.acento)
        for (i in 0 until puntos) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(if (i < restantes) c.acento else Color.Transparent, CircleShape)
                    .border(1.5.dp, c.acento, CircleShape),
            )
        }
    }
}

// Las cinco puertas de la app, de izquierda a derecha. Batallas va al centro, con su botón redondo.
enum class DestinoMC(@StringRes val nombre: Int, val icono: ImageVector) {
    Diario(R.string.nav_diario, Icons.Rounded.Code),
    Dojo(R.string.nav_dojo, Icons.Rounded.MilitaryTech),
    Batallas(R.string.nav_batallas, Icons.Rounded.LocalFireDepartment),
    Cursos(R.string.nav_cursos, Icons.Rounded.School),
    Perfil(R.string.nav_perfil, Icons.Rounded.Person),
}

private val ALTO_DE_LA_BARRA = 92.dp
private val LINEA_DE_LA_BARRA = 26.dp

// La barra de navegación inferior de Consoly: una línea fina arriba, íconos con su nombre en mayúsculas y, en el centro, el botón
// redondo de Batallas con su anillo naranja, que sobresale de la línea. No se aparta de la barra del sistema: eso lo hace PantallaMC.
@Composable
fun BarraInferiorMC(seleccion: DestinoMC, alElegir: (DestinoMC) -> Unit, modifier: Modifier = Modifier) {
    val c = MC.colores
    // Una vibración muy suave al cambiar de pestaña (no al tocar la que ya está elegida).
    val vibracion = LocalHapticFeedback.current
    val elegir: (DestinoMC) -> Unit = { destino ->
        if (destino != seleccion) vibracion.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        alElegir(destino)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ALTO_DE_LA_BARRA)
            .drawBehind {
                val y = LINEA_DE_LA_BARRA.toPx()
                drawLine(c.borde, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            },
        verticalAlignment = Alignment.Bottom,
    ) {
        for (destino in DestinoMC.values()) {
            val activo = destino == seleccion
            if (destino == DestinoMC.Batallas) {
                BotonDeBatalla(activo, { elegir(destino) }, Modifier.weight(1f))
            } else {
                ItemDeBarra(destino, activo, { elegir(destino) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ItemDeBarra(destino: DestinoMC, activo: Boolean, alTocar: () -> Unit, modifier: Modifier) {
    val c = MC.colores
    val color by animateColorAsState(targetValue = if (activo) c.acento else c.textoSuave, animationSpec = tween(durationMillis = 200), label = "colorDePestana")
    val escala by animateFloatAsState(
        targetValue = if (activo) 1.14f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "escalaDePestana",
    )
    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .selectable(selected = activo, role = Role.Tab, onClick = alTocar)
            .padding(top = 6.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            destino.icono,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer {
                    scaleX = escala
                    scaleY = escala
                },
        )
        Text(
            stringResource(destino.nombre).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.8.sp),
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun BotonDeBatalla(activo: Boolean, alTocar: () -> Unit, modifier: Modifier) {
    val c = MC.colores
    Column(
        modifier = modifier
            .selectable(selected = activo, role = Role.Tab, onClick = alTocar)
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .resplandor(NaranjaDeBatalla, 31.dp, intensidad = 1.3f)
                .background(FondoDeBatalla, CircleShape)
                .border(1.dp, NaranjaDeBatalla.copy(alpha = 0.35f), CircleShape)
                .anilloGiratorio(
                    listOf(NaranjaDeBatalla, FuegoDeRacha, NaranjaDeBatalla.copy(alpha = 0.1f), NaranjaDeBatalla),
                    grosor = 2.5.dp,
                    periodoMs = 2600,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = if (activo) FuegoDeRacha else NaranjaDeBatalla,
                modifier = Modifier.size(34.dp),
            )
        }
        Text(
            stringResource(DestinoMC.Batallas.nombre).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.8.sp),
            color = if (activo) c.acento else c.textoSuave,
            maxLines = 1,
        )
    }
}

// La tarjeta dorada del «DESGLOSE DE PUNTOS»: fondo marrón con degradado, borde dorado y un resplandor.
@Composable
fun TarjetaDorada(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    val forma = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .shadow(16.dp, forma, ambientColor = Dorado, spotColor = Dorado)
            .background(Brush.linearGradient(listOf(DoradoFondoA, DoradoFondoB)), forma)
            .border(1.dp, Dorado.copy(alpha = 0.7f), forma)
            .brilloQueCruza(forma, intensidad = 0.16f, periodoMs = 3800)
            .padding(20.dp),
        content = contenido,
    )
}

// El banner con degradado azul a violeta de Consoly («Resuelve 4 katas más para ganar un bono»): aquí lleva el mensaje del resultado.
@Composable
fun BannerMC(texto: String, modifier: Modifier = Modifier) {
    val c = MC.colores
    val forma = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(BannerAzul, BannerVioleta)), forma)
            .border(1.dp, c.acento.copy(alpha = 0.5f), forma)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, style = MaterialTheme.typography.bodyLarge, color = Color.White, textAlign = TextAlign.Center)
    }
}
