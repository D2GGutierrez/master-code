package com.mastercode.app.ui.duelos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R
import com.mastercode.app.ui.componentes.AvatarMC
import com.mastercode.app.ui.componentes.BotonDeTexto
import com.mastercode.app.ui.componentes.BotonPrincipal
import com.mastercode.app.ui.componentes.BotonSecundario
import com.mastercode.app.ui.componentes.EstadoVacio
import com.mastercode.app.ui.componentes.MensajeDeError
import com.mastercode.app.ui.componentes.TarjetaMC

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
            MensajeDeError(stringResource(it), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        when (val contenido = estado.contenido) {
            ContenidoDeInvitaciones.Cargando -> CargandoDuelos()
            ContenidoDeInvitaciones.Vacio -> EstadoVacio(
                ilustracion = R.drawable.ilu_vacio_invitaciones,
                texto = stringResource(R.string.invitaciones_vacio),
                accion = {
                    BotonDeTexto(stringResource(R.string.invitaciones_actualizar), viewModel::cargar)
                },
            )
            is ContenidoDeInvitaciones.Error -> ErrorConReintento(contenido.mensaje, viewModel::cargar)
            is ContenidoDeInvitaciones.Lista -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    BotonSecundario(
                        texto = stringResource(R.string.invitaciones_actualizar),
                        alTocar = viewModel::cargar,
                        icono = Icons.Rounded.Refresh,
                        compacto = true,
                    )
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                    items(contenido.invitaciones, key = { it.id }) { invitacion ->
                        TarjetaMC(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    AvatarMC(invitacion.retador)
                                    Text(
                                        stringResource(R.string.invitaciones_de, invitacion.retador),
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    BotonPrincipal(
                                        texto = stringResource(R.string.invitaciones_aceptar),
                                        alTocar = { viewModel.aceptar(invitacion) },
                                        habilitado = estado.procesando == null,
                                        compacto = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                    BotonSecundario(
                                        texto = stringResource(R.string.invitaciones_rechazar),
                                        alTocar = { viewModel.rechazar(invitacion) },
                                        habilitado = estado.procesando == null,
                                        compacto = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
