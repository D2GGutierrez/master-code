package com.mastercode.app.ui.duelos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R
import com.mastercode.app.data.duelos.PartidaDelHistorial
import com.mastercode.app.ui.componentes.BotonPrincipal
import com.mastercode.app.ui.componentes.BotonSecundario
import com.mastercode.app.ui.componentes.EstadoVacio
import com.mastercode.app.ui.componentes.InsigniaMC
import com.mastercode.app.ui.componentes.MensajeDeError
import com.mastercode.app.ui.componentes.TarjetaMC
import com.mastercode.app.ui.componentes.TonoDeInsignia
import com.mastercode.app.ui.theme.EstiloDeCodigo
import com.mastercode.app.ui.theme.MC

// HU60-T3: el historial de partidas, la más reciente primero, de a 20 con «Ver más». Una partida en curso se puede retomar.
@Composable
fun HistorialTab(viewModel: HistorialViewModel, alAbrirPartida: (Long) -> Unit, alVencerSesion: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    AlVencerLaSesion(estado.sesionVencida, alVencerSesion)

    when {
        estado.cargando || estado.sesionVencida -> CargandoDuelos()
        estado.error != null && estado.partidas.isEmpty() -> ErrorConReintento(estado.error!!, viewModel::cargar)
        estado.vacio -> EstadoVacio(ilustracion = R.drawable.ilu_vacio_historial, texto = stringResource(R.string.historial_vacio))
        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(estado.partidas, key = { it.id }) { partida -> FilaDePartida(partida, alAbrirPartida) }
            item {
                // Al fallar una página, lo ya traído se conserva y se puede volver a pedir.
                estado.error?.let {
                    MensajeDeError(stringResource(it), modifier = Modifier.padding(vertical = 8.dp))
                }
                if (estado.cargandoMas) {
                    val descripcion = stringResource(R.string.duelos_cargando)
                    CircularProgressIndicator(modifier = Modifier.size(28.dp).semantics { contentDescription = descripcion })
                } else if (estado.hayMas) {
                    BotonSecundario(
                        texto = stringResource(R.string.historial_ver_mas),
                        alTocar = viewModel::cargarMas,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaDePartida(partida: PartidaDelHistorial, alContinuar: (Long) -> Unit) {
    val c = MC.colores
    val (tono, icono) = when (partida.estado) {
        "terminada" -> when (partida.resultado) {
            "gane" -> TonoDeInsignia.Exito to Icons.Rounded.CheckCircle
            "perdi" -> TonoDeInsignia.Error to null
            else -> TonoDeInsignia.Neutro to null
        }
        "en_curso" -> TonoDeInsignia.Acento to Icons.Rounded.PlayArrow
        else -> TonoDeInsignia.Neutro to null
    }
    TarjetaMC(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.historial_contra, partida.rival), style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InsigniaMC(stringResource(etiquetaDeLaPartida(partida)), tono = tono, icono = icono)
                        if (partida.motivoFin == "abandono") {
                            Text(stringResource(R.string.historial_abandono), style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                        }
                    }
                    fechaCorta(partida.fecha)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.textoSuave) }
                }
                // El marcador grande y en letra de código; el lector de pantalla lo oye como «3 a 2».
                val marcador = stringResource(R.string.historial_marcador, partida.miPuntaje, partida.puntajeRival)
                Text(
                    "${partida.miPuntaje} - ${partida.puntajeRival}",
                    style = EstiloDeCodigo.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.semantics { contentDescription = marcador },
                )
            }
            if (sePuedeContinuar(partida)) {
                BotonPrincipal(
                    texto = stringResource(R.string.historial_continuar),
                    alTocar = { alContinuar(partida.id) },
                    compacto = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
