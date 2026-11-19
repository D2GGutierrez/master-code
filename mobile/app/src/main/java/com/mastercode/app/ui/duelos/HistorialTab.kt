package com.mastercode.app.ui.duelos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R
import com.mastercode.app.data.duelos.PartidaDelHistorial

// HU60-T3: el historial de partidas, la más reciente primero, de a 20 con «Ver más». Una partida en curso se puede retomar.
@Composable
fun HistorialTab(viewModel: HistorialViewModel, alAbrirPartida: (Long) -> Unit, alVencerSesion: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    AlVencerLaSesion(estado.sesionVencida, alVencerSesion)

    when {
        estado.cargando || estado.sesionVencida -> CargandoDuelos()
        estado.error != null && estado.partidas.isEmpty() -> ErrorConReintento(estado.error!!, viewModel::cargar)
        estado.vacio -> CentradoDuelos { Text(stringResource(R.string.historial_vacio), textAlign = TextAlign.Center) }
        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
            items(estado.partidas, key = { it.id }) { partida -> FilaDePartida(partida, alAbrirPartida) }
            item {
                // Al fallar una página, lo ya traído se conserva y se puede volver a pedir.
                estado.error?.let {
                    Text(stringResource(it), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
                }
                if (estado.cargandoMas) {
                    val descripcion = stringResource(R.string.duelos_cargando)
                    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = descripcion })
                } else if (estado.hayMas) {
                    OutlinedButton(
                        onClick = viewModel::cargarMas,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.historial_ver_mas)) }
                }
            }
        }
    }
}

@Composable
private fun FilaDePartida(partida: PartidaDelHistorial, alContinuar: (Long) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.historial_contra, partida.rival), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(etiquetaDeLaPartida(partida)), style = MaterialTheme.typography.bodyLarge)
                if (partida.motivoFin == "abandono") {
                    Text(stringResource(R.string.historial_abandono), style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    stringResource(R.string.historial_marcador, partida.miPuntaje, partida.puntajeRival),
                    style = MaterialTheme.typography.bodyMedium,
                )
                fechaCorta(partida.fecha)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            if (sePuedeContinuar(partida)) {
                Button(onClick = { alContinuar(partida.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.historial_continuar))
                }
            }
        }
    }
}
