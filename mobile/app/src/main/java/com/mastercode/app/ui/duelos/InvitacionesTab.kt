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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R

// H7: los retos que otros compañeros le hicieron al estudiante. «Aceptar» abre la partida; «Rechazar» la quita de la lista.
@Composable
fun InvitacionesTab(viewModel: InvitacionesViewModel, alAbrirPartida: (Long) -> Unit, alVencerSesion: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    AlVencerLaSesion(estado.sesionVencida, alVencerSesion)
    LaunchedEffect(estado.partidaAceptada) {
        estado.partidaAceptada?.let {
            alAbrirPartida(it)
            viewModel.alAbrirseLaPartida()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        estado.mensaje?.let {
            Text(
                stringResource(it),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        when (val contenido = estado.contenido) {
            ContenidoDeInvitaciones.Cargando -> CargandoDuelos()
            ContenidoDeInvitaciones.Vacio -> CentradoDuelos {
                Text(stringResource(R.string.invitaciones_vacio), textAlign = TextAlign.Center)
                TextButton(onClick = viewModel::cargar, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.invitaciones_actualizar))
                }
            }
            is ContenidoDeInvitaciones.Error -> ErrorConReintento(contenido.mensaje, viewModel::cargar)
            is ContenidoDeInvitaciones.Lista -> {
                TextButton(onClick = viewModel::cargar, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.invitaciones_actualizar))
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(contenido.invitaciones, key = { it.id }) { invitacion ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.invitaciones_de, invitacion.retador), style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.aceptar(invitacion) },
                                        enabled = estado.procesando == null,
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    ) { Text(stringResource(R.string.invitaciones_aceptar)) }
                                    OutlinedButton(
                                        onClick = { viewModel.rechazar(invitacion) },
                                        enabled = estado.procesando == null,
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    ) { Text(stringResource(R.string.invitaciones_rechazar)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
