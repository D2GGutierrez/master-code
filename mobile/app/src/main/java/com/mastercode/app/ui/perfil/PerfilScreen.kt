package com.mastercode.app.ui.perfil

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R
import com.mastercode.app.data.cinturon.CinturonDelLenguaje
import com.mastercode.app.ui.cinturon.nombreDeCinturon
import com.mastercode.app.ui.componentes.BarraDeProgreso
import com.mastercode.app.ui.componentes.BotonSecundario
import com.mastercode.app.ui.componentes.CabeceraMC
import com.mastercode.app.ui.componentes.EsqueletoDeTarjetas
import com.mastercode.app.ui.componentes.EstadoVacio
import com.mastercode.app.ui.componentes.LogoLenguaje
import com.mastercode.app.ui.componentes.MensajeDeError
import com.mastercode.app.ui.componentes.PantallaMC
import com.mastercode.app.ui.componentes.TarjetaMC
import com.mastercode.app.ui.theme.Dorado
import com.mastercode.app.ui.theme.MC
import com.mastercode.app.ui.theme.Oro
import com.mastercode.app.ui.theme.paletaDe
import com.mastercode.app.ui.componentes.entrada

// HU51-T2 y HU51-T3 (CA-51.2 a CA-51.4): el perfil del estudiante. Un cinturón por lenguaje con su imagen (CA-50.4), la barra de avance
// hacia el siguiente y, con el negro, un mensaje especial en lugar de la barra. Es una pestaña de la barra inferior (pie); desde
// aquí también se llega a los avisos de práctica.
@Composable
fun PerfilScreen(
    viewModel: PerfilViewModel,
    alAbrirAvisos: () -> Unit,
    alVencerSesion: () -> Unit,
    pie: @Composable () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.contenido) {
        if (uiState.contenido == ContenidoPerfil.SesionVencida) alVencerSesion()
    }

    PantallaMC(
        cabecera = {
            CabeceraMC(
                titulo = stringResource(R.string.perfil_titulo),
                sobretitulo = stringResource(R.string.perfil_sobretitulo),
            )
        },
        pie = pie,
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        ) {
            when (val contenido = uiState.contenido) {
                ContenidoPerfil.Cargando, ContenidoPerfil.SesionVencida -> item {
                    EsqueletoDeTarjetas(
                        descripcion = stringResource(R.string.perfil_cargando),
                        cantidad = 2,
                        alto = 150.dp,
                    )
                }
                ContenidoPerfil.SinCinturones -> item {
                    EstadoVacio(
                        ilustracion = R.drawable.ilu_mascota,
                        texto = stringResource(R.string.perfil_vacio),
                    )
                }
                is ContenidoPerfil.Error -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MensajeDeError(stringResource(contenido.mensaje))
                        BotonSecundario(
                            texto = stringResource(R.string.perfil_reintentar),
                            alTocar = viewModel::cargar,
                            icono = Icons.Rounded.Refresh,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                is ContenidoPerfil.ConCinturones -> itemsIndexed(contenido.cinturones, key = { _, cinturon -> cinturon.lenguaje }) { indice, cinturon ->
                    TarjetaDeCinturon(cinturon, if (indice < 6) Modifier.entrada(indice * 70) else Modifier)
                }
            }
            item { TarjetaDeAvisos(alAbrirAvisos) }
        }
    }
}

// La puerta a los avisos de práctica, que antes estaba en «Mis cursos».
@Composable
private fun TarjetaDeAvisos(alTocar: () -> Unit) {
    val c = MC.colores
    TarjetaMC(modifier = Modifier.fillMaxWidth(), alTocar = alTocar) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = c.acento, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.perfil_avisos_titulo), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.perfil_avisos_texto), style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textoSuave, modifier = Modifier.size(24.dp))
        }
    }
}

// La imagen de cada cinturón (res/drawable-nodpi/img_cinturon_*.webp, HU50-T3), por nivel.
@DrawableRes
internal fun imagenDelCinturon(nivel: Int): Int = when (nivel) {
    1 -> R.drawable.img_cinturon_blanco
    2 -> R.drawable.img_cinturon_amarillo
    3 -> R.drawable.img_cinturon_naranja
    4 -> R.drawable.img_cinturon_verde
    5 -> R.drawable.img_cinturon_azul
    6 -> R.drawable.img_cinturon_marron
    else -> R.drawable.img_cinturon_negro
}

@Composable
private fun nombreLocalizado(nivel: Int, delServidor: String): String =
    nombreDeCinturon(nivel)?.let { stringResource(it) } ?: delServidor

@Composable
private fun TarjetaDeCinturon(cinturon: CinturonDelLenguaje, modifier: Modifier = Modifier) {
    val c = MC.colores
    val paleta = paletaDe(cinturon.lenguaje)
    TarjetaMC(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // El cinturón sobre un cuadro gris con borde fino.
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .background(MC.colores.lienzo, RoundedCornerShape(16.dp))
                        .border(1.dp, MC.colores.borde, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(imagenDelCinturon(cinturon.nivel)),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LogoLenguaje(cinturon.lenguaje, 20.dp)
                        Text(
                            stringResource(R.string.perfil_cinturon_de, nombreLocalizado(cinturon.nivel, cinturon.nombre), cinturon.lenguaje),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        stringResource(R.string.perfil_puntos_totales, cinturon.puntos),
                        style = MaterialTheme.typography.titleLarge,
                        color = Dorado,
                    )
                }
            }
            val siguiente = cinturon.siguiente
            val avance = cinturon.avance
            if (siguiente == null || avance == null) {
                // HU51-T3 (CA-51.4): con el cinturón negro no hay barra, hay un mensaje especial.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Oro, modifier = Modifier.size(28.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.perfil_negro_titulo), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.perfil_negro_mensaje), style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
                    }
                }
            } else {
                // HU51-T2 (CA-51.3): la barra de avance hacia el siguiente cinturón.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BarraDeProgreso(progreso = avance, descripcion = stringResource(R.string.perfil_barra_descripcion), alto = 10.dp)
                    Text(
                        stringResource(
                            R.string.perfil_puntos,
                            cinturon.puntos,
                            siguiente.puntosRequeridos,
                            nombreLocalizado(siguiente.nivel, siguiente.nombre),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    cinturon.puntosFaltantes?.let {
                        Text(
                            if (it == 1) stringResource(R.string.perfil_faltan_uno) else stringResource(R.string.perfil_faltan, it),
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textoSuave,
                        )
                    }
                }
            }
        }
    }
}
