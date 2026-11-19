package com.mastercode.app.ui.perfil

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
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
import com.mastercode.app.ui.theme.MC
import com.mastercode.app.ui.theme.Oro
import com.mastercode.app.ui.theme.paletaDe

// HU51-T2 y HU51-T3 (CA-51.2 a CA-51.4): el perfil del estudiante. Un cinturón por lenguaje con su imagen (CA-50.4), la barra
// de avance hacia el siguiente y, con el negro, un mensaje especial en lugar de la barra.
@Composable
fun PerfilScreen(viewModel: PerfilViewModel, alVolver: () -> Unit, alVencerSesion: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(onBack = alVolver)
    LaunchedEffect(uiState.contenido) {
        if (uiState.contenido == ContenidoPerfil.SesionVencida) alVencerSesion()
    }

    PantallaMC(
        cabecera = {
            CabeceraMC(
                titulo = stringResource(R.string.perfil_titulo),
                sobretitulo = stringResource(R.string.perfil_sobretitulo),
                alVolver = alVolver,
            )
        },
    ) {
        when (val contenido = uiState.contenido) {
            ContenidoPerfil.Cargando, ContenidoPerfil.SesionVencida -> EsqueletoDeTarjetas(
                descripcion = stringResource(R.string.perfil_cargando),
                modifier = Modifier.padding(20.dp),
                cantidad = 2,
                alto = 150.dp,
            )
            ContenidoPerfil.SinCinturones -> EstadoVacio(
                ilustracion = R.drawable.ilu_mascota,
                texto = stringResource(R.string.perfil_vacio),
                modifier = Modifier.padding(20.dp),
            )
            is ContenidoPerfil.Error -> Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MensajeDeError(stringResource(contenido.mensaje))
                BotonSecundario(
                    texto = stringResource(R.string.perfil_reintentar),
                    alTocar = viewModel::cargar,
                    icono = Icons.Rounded.Refresh,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            is ContenidoPerfil.ConCinturones -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
            ) {
                items(contenido.cinturones, key = { it.lenguaje }) { TarjetaDeCinturon(it) }
            }
        }
    }
}

// La imagen de cada cinturón (res/drawable-nodpi/img_cinturon_*.webp, HU50-T3), por nivel.
@DrawableRes
private fun imagenDelCinturon(nivel: Int): Int = when (nivel) {
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
private fun TarjetaDeCinturon(cinturon: CinturonDelLenguaje) {
    val c = MC.colores
    val paleta = paletaDe(cinturon.lenguaje)
    TarjetaMC(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // El cinturón sobre un cuadro gris con borde fino.
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(MC.colores.fondoSuave, RoundedCornerShape(8.dp))
                        .border(1.dp, MC.colores.borde, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(imagenDelCinturon(cinturon.nivel)),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textoSuave,
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
