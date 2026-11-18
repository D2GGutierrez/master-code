package com.mastercode.app.ui.perfil

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R
import com.mastercode.app.data.cinturon.CinturonDelLenguaje
import com.mastercode.app.ui.cinturon.nombreDeCinturon

// HU51-T2 y HU51-T3 (CA-51.2 a CA-51.4): el perfil del estudiante. Un cinturón por lenguaje con su imagen (CA-50.4), la barra
// de avance hacia el siguiente y, con el negro, un mensaje especial en lugar de la barra.
@Composable
fun PerfilScreen(viewModel: PerfilViewModel, alVolver: () -> Unit, alVencerSesion: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(onBack = alVolver)
    LaunchedEffect(uiState.contenido) {
        if (uiState.contenido == ContenidoPerfil.SesionVencida) alVencerSesion()
    }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp)) {
        Text(
            stringResource(R.string.perfil_titulo),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val contenido = uiState.contenido) {
                ContenidoPerfil.Cargando, ContenidoPerfil.SesionVencida -> Centrado {
                    val descripcion = stringResource(R.string.perfil_cargando)
                    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = descripcion })
                }
                ContenidoPerfil.SinCinturones -> Centrado {
                    Text(stringResource(R.string.perfil_vacio), textAlign = TextAlign.Center)
                }
                is ContenidoPerfil.Error -> Centrado {
                    Text(stringResource(contenido.mensaje), textAlign = TextAlign.Center)
                    Button(onClick = viewModel::cargar) { Text(stringResource(R.string.perfil_reintentar)) }
                }
                is ContenidoPerfil.ConCinturones -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(contenido.cinturones, key = { it.lenguaje }) { TarjetaDeCinturon(it) }
                }
            }
        }
        TextButton(
            onClick = alVolver,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 8.dp),
        ) { Text(stringResource(R.string.perfil_volver)) }
    }
}

// La imagen de cada cinturón (res/drawable/cinturon_*.xml, HU50-T3), por nivel.
@DrawableRes
private fun imagenDelCinturon(nivel: Int): Int = when (nivel) {
    1 -> R.drawable.cinturon_blanco
    2 -> R.drawable.cinturon_amarillo
    3 -> R.drawable.cinturon_naranja
    4 -> R.drawable.cinturon_verde
    5 -> R.drawable.cinturon_azul
    6 -> R.drawable.cinturon_marron
    else -> R.drawable.cinturon_negro
}

@Composable
private fun nombreLocalizado(nivel: Int, delServidor: String): String =
    nombreDeCinturon(nivel)?.let { stringResource(it) } ?: delServidor

@Composable
private fun TarjetaDeCinturon(cinturon: CinturonDelLenguaje) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(
                    painter = painterResource(imagenDelCinturon(cinturon.nivel)),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                )
                Column {
                    Text(
                        stringResource(R.string.perfil_cinturon_de, nombreLocalizado(cinturon.nivel, cinturon.nombre), cinturon.lenguaje),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(stringResource(R.string.perfil_puntos_totales, cinturon.puntos), style = MaterialTheme.typography.bodySmall)
                }
            }
            val siguiente = cinturon.siguiente
            val avance = cinturon.avance
            if (siguiente == null || avance == null) {
                // HU51-T3 (CA-51.4): con el cinturón negro no hay barra, hay un mensaje especial.
                Text(stringResource(R.string.perfil_negro_titulo), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.perfil_negro_mensaje))
            } else {
                // HU51-T2 (CA-51.3): la barra de avance hacia el siguiente cinturón.
                val descripcion = stringResource(R.string.perfil_barra_descripcion)
                LinearProgressIndicator(
                    progress = { avance },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = descripcion },
                )
                Text(
                    stringResource(
                        R.string.perfil_puntos,
                        cinturon.puntos,
                        siguiente.puntosRequeridos,
                        nombreLocalizado(siguiente.nivel, siguiente.nombre),
                    ),
                )
                cinturon.puntosFaltantes?.let {
                    Text(if (it == 1) stringResource(R.string.perfil_faltan_uno) else stringResource(R.string.perfil_faltan, it))
                }
            }
        }
    }
}

@Composable
private fun Centrado(contenido: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { contenido() }
    }
}
