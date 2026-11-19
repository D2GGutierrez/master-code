package com.mastercode.app.ui.componentes

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.ErrorOutline
import com.mastercode.app.ui.theme.MC

// La tarjeta de siempre: fondo del tema, borde fino, radio de 8 y, si se pasa alTocar, toda ella es tocable. Resaltada pinta
// el acento suave y un borde de acento (la fila propia del ranking, la opción elegida).
@Composable
fun TarjetaMC(
    modifier: Modifier = Modifier,
    alTocar: (() -> Unit)? = null,
    etiquetaDeToque: String? = null,
    resaltada: Boolean = false,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val c = MC.colores
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = if (resaltada) c.acentoSuave else c.fondo,
        contentColor = c.texto,
        border = BorderStroke(if (resaltada) 1.5.dp else 1.dp, if (resaltada) c.acento else c.borde),
    ) {
        val base = if (alTocar != null) {
            Modifier.clickable(onClickLabel = etiquetaDeToque, role = Role.Button, onClick = alTocar)
        } else {
            Modifier
        }
        Column(modifier = base, content = contenido)
    }
}

enum class TonoDeInsignia { Acento, Exito, Error, Neutro }

// Una etiqueta pequeña y redonda («Python», «En curso», «Ganaste»…). Verde y rojo solo de texto, sobre un fondo neutro.
@Composable
fun InsigniaMC(
    texto: String,
    modifier: Modifier = Modifier,
    tono: TonoDeInsignia = TonoDeInsignia.Neutro,
    icono: ImageVector? = null,
) {
    val c = MC.colores
    val colorDelTexto = when (tono) {
        TonoDeInsignia.Acento -> c.acento
        TonoDeInsignia.Exito -> c.exito
        TonoDeInsignia.Error -> c.error
        TonoDeInsignia.Neutro -> c.textoSuave
    }
    val fondo = if (tono == TonoDeInsignia.Acento) c.acentoSuave else c.fondoSuave
    Row(
        modifier = modifier
            .background(fondo, CircleShape)
            .border(1.dp, c.borde, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, tint = colorDelTexto, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelMedium, color = colorDelTexto)
    }
}

// El círculo con la inicial de una persona (compañeros, invitaciones). Es decorativo: el nombre va al lado, en texto.
@Composable
fun AvatarMC(nombre: String, modifier: Modifier = Modifier, tamano: Dp = 44.dp) {
    val inicial = nombre.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = modifier
            .size(tamano)
            .background(Color(0xFF6E7781), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(inicial, style = MaterialTheme.typography.titleMedium, color = Color.White, fontSize = (tamano.value * 0.4f).sp)
    }
}

// Cuando no hay nada que mostrar: borde discontinuo, una ilustración (o un ícono) y un texto; opcionalmente, una acción.
@Composable
fun EstadoVacio(
    texto: String,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    @DrawableRes ilustracion: Int? = null,
    titulo: String? = null,
    accion: (@Composable () -> Unit)? = null,
) {
    val c = MC.colores
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = c.bordeFuerte,
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)),
                )
            }
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (ilustracion != null) {
            Image(painter = painterResource(ilustracion), contentDescription = null, modifier = Modifier.size(150.dp))
        } else if (icono != null) {
            Box(modifier = Modifier.size(56.dp).background(c.acentoSuave, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icono, contentDescription = null, tint = c.acento, modifier = Modifier.size(28.dp))
            }
        }
        if (titulo != null) Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = c.textoSuave, textAlign = TextAlign.Center)
        accion?.invoke()
    }
}

// Un mensaje de error en línea: borde y texto de error sobre el fondo normal.
@Composable
fun MensajeDeError(texto: String, modifier: Modifier = Modifier) {
    val c = MC.colores
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.fondo, RoundedCornerShape(6.dp))
            .border(1.dp, c.error.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = c.error, modifier = Modifier.size(20.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = c.error, modifier = Modifier.weight(1f))
    }
}
