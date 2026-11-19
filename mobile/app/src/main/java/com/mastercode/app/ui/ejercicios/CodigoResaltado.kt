package com.mastercode.app.ui.ejercicios

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mastercode.app.R
import com.mastercode.app.ui.componentes.EtiquetaMC
import com.mastercode.app.ui.theme.Barra
import com.mastercode.app.ui.theme.BordeSobreHero
import com.mastercode.app.ui.theme.CodigoComentario
import com.mastercode.app.ui.theme.CodigoLlamada
import com.mastercode.app.ui.theme.CodigoNormal
import com.mastercode.app.ui.theme.CodigoNumero
import com.mastercode.app.ui.theme.CodigoNumeroDeLinea
import com.mastercode.app.ui.theme.CodigoPalabra
import com.mastercode.app.ui.theme.CodigoSigno
import com.mastercode.app.ui.theme.CodigoTexto
import com.mastercode.app.ui.theme.EstiloDeCodigo
import com.mastercode.app.ui.theme.MC
import androidx.compose.foundation.shape.CircleShape
import com.mastercode.app.ui.theme.VentanaAmarillo
import com.mastercode.app.ui.theme.VentanaRojo
import com.mastercode.app.ui.theme.VentanaVerde

private const val TAMANO_INICIAL = 14
private const val TAMANO_MINIMO = 10
private const val TAMANO_MAXIMO = 22

private fun colorDelToken(tipo: TipoDeToken): Color = when (tipo) {
    TipoDeToken.Palabra -> CodigoPalabra
    TipoDeToken.Texto -> CodigoTexto
    TipoDeToken.Numero -> CodigoNumero
    TipoDeToken.Comentario -> CodigoComentario
    TipoDeToken.Llamada -> CodigoLlamada
    TipoDeToken.Signo -> CodigoSigno
    TipoDeToken.Normal -> CodigoNormal
}

// El texto con los colores de un editor oscuro. Varias líneas: cada una se pinta por separado.
fun resaltar(texto: String): AnnotatedString = buildAnnotatedString {
    val lineas = texto.split("\n")
    lineas.forEachIndexed { indice, linea ->
        for (token in tokenizarLinea(linea)) {
            withStyle(SpanStyle(color = colorDelToken(token.tipo))) { append(token.texto) }
        }
        if (indice < lineas.size - 1) append("\n")
    }
}

// Un fragmento de código como en un editor: fondo oscuro, letra de ancho fijo y, si una línea no cabe, se desliza de lado en
// vez de cortarse.
@Composable
fun BloqueDeCodigo(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    val forma = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .clip(forma)
            .background(Barra)
            .border(1.dp, BordeSobreHero, forma)
            .horizontalScroll(rememberScrollState())
            .padding(16.dp),
    ) { contenido() }
}

// Código con colores y sin números de línea (lo usa «completar el espacio», donde el campo va en medio de la línea).
@Composable
fun TextoDeCodigo(texto: String) {
    Text(resaltar(texto), style = EstiloDeCodigo.copy(color = CodigoNormal))
}

// Las líneas numeradas y con colores, con el tamaño de letra que elige el estudiante.
@Composable
fun CodigoNumerado(texto: String, tamano: Int = TAMANO_INICIAL, modifier: Modifier = Modifier) {
    val estilo = EstiloDeCodigo.copy(fontSize = tamano.sp, lineHeight = (tamano * 1.6f).sp, color = CodigoNormal)
    val lineas = texto.split("\n")
    Column(modifier = modifier) {
        lineas.forEachIndexed { indice, linea ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    (indice + 1).toString(),
                    style = estilo.copy(color = CodigoNumeroDeLinea),
                    textAlign = TextAlign.End,
                    modifier = Modifier.width((tamano * 2).dp),
                )
                Spacer(Modifier.width(14.dp))
                Text(resaltar(linea), style = estilo)
            }
        }
    }
}

// El panel de código de Consoly: una barra con el nombre y los botones de zoom (Z− y Z+), y debajo el código numerado.
@Composable
fun PanelDeCodigo(texto: String, modifier: Modifier = Modifier) {
    val c = MC.colores
    var tamano by rememberSaveable { mutableIntStateOf(TAMANO_INICIAL) }
    val forma = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(forma)
            .background(Barra)
            .border(1.dp, BordeSobreHero, forma),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(c.fondo).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Los tres puntos de una ventana de editor, como en las tarjetas del Diario.
            for (punto in listOf(VentanaRojo, VentanaAmarillo, VentanaVerde)) {
                Box(modifier = Modifier.size(10.dp).background(punto, CircleShape))
            }
            EtiquetaMC(stringResource(R.string.codigo_etiqueta), color = c.texto, modifier = Modifier.padding(start = 6.dp).weight(1f))
            BotonDeZoom(Icons.Rounded.ZoomOut, stringResource(R.string.codigo_alejar)) {
                tamano = if (tamano > TAMANO_MINIMO) tamano - 1 else TAMANO_MINIMO
            }
            BotonDeZoom(Icons.Rounded.ZoomIn, stringResource(R.string.codigo_acercar)) {
                tamano = if (tamano < TAMANO_MAXIMO) tamano + 1 else TAMANO_MAXIMO
            }
        }
        Box(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(16.dp)) {
            CodigoNumerado(texto, tamano)
        }
    }
}

@Composable
private fun BotonDeZoom(icono: ImageVector, descripcion: String, alTocar: () -> Unit) {
    val c = MC.colores
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.fondoSuave)
            .clickable(onClickLabel = descripcion, role = Role.Button, onClick = alTocar),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icono, contentDescription = null, tint = c.textoSuave, modifier = Modifier.size(22.dp))
    }
}
