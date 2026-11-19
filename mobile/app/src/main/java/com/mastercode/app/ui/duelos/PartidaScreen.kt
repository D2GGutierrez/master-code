package com.mastercode.app.ui.duelos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R
import com.mastercode.app.data.duelos.JugadorDelMarcador
import com.mastercode.app.data.duelos.Marcador
import com.mastercode.app.ui.ejercicios.CompletarEspacioScreen
import com.mastercode.app.ui.ejercicios.EstadoDeEnvio
import com.mastercode.app.ui.ejercicios.OpcionMultipleScreen
import com.mastercode.app.ui.ejercicios.PredecirSalidaScreen
import com.mastercode.app.ui.ejercicios.TIPO_COMPLETAR_ESPACIO
import com.mastercode.app.ui.ejercicios.TIPO_OPCION_MULTIPLE
import com.mastercode.app.ui.ejercicios.TIPO_PREDICCION_SALIDA

// H8 y HU59-T3: la pantalla de la partida. Arriba va el marcador en vivo (puntaje, ejercicios respondidos y si el rival está
// conectado), que se actualiza solo cada 2 s; abajo, el ejercicio del turno con las mismas pantallas de ejercicio del resto
// de la app. Al responder se ve la corrección y «Siguiente ejercicio»; al terminar, el resultado. Salir en mitad de una
// partida pide confirmar: user-api da la partida por abandonada si el estudiante no vuelve en 30 s.
@Composable
fun PartidaScreen(viewModel: PartidaViewModel, alVolver: () -> Unit, alVencerSesion: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmandoSalida by rememberSaveable { mutableStateOf(false) }

    val fase = estado.fase
    val marcador = estado.marcador
    // Solo se confirma al salir si la partida está en juego.
    val enJuego = marcador?.estado == "en_curso" && fase !is FaseDePartida.Terminada && fase !is FaseDePartida.Cerrada
    val salir = {
        if (enJuego) {
            confirmandoSalida = true
        } else {
            alVolver()
        }
    }

    BackHandler(onBack = salir)
    // El marcador se consulta solo mientras la pantalla está a la vista: al salir o pasar la app a segundo plano se deja de
    // dar señal, y user-api da la partida por abandonada si no se vuelve en 30 s.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.reanudar() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.pausar() }
    DisposableEffect(viewModel) { onDispose { viewModel.pausar() } }
    LaunchedEffect(fase) {
        if (fase == FaseDePartida.SesionVencida) alVencerSesion()
    }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        if (fase !is FaseDePartida.Terminada) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.partida_titulo, marcador?.rival?.alias ?: ""),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = salir, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.partida_salir)) }
            }
            if (marcador != null && fase !is FaseDePartida.Cerrada && fase != FaseDePartida.EsperandoRival) MarcadorEnVivo(marcador)
        }
        if (estado.sinSenal) {
            Text(
                stringResource(R.string.partida_sin_senal),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (fase) {
                FaseDePartida.Cargando, FaseDePartida.SesionVencida -> CentradoPartida {
                    val descripcion = stringResource(R.string.partida_cargando)
                    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = descripcion })
                }
                FaseDePartida.EsperandoRival -> CentradoPartida {
                    CircularProgressIndicator()
                    Text(
                        stringResource(R.string.partida_esperando_rival, marcador?.rival?.alias ?: ""),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                is FaseDePartida.Jugando -> EjercicioDeLaPartida(
                    fase = fase,
                    finDeLaPartida = marcador?.estado == "terminada",
                    alSiguiente = viewModel::siguiente,
                    alVencerSesion = alVencerSesion,
                )
                FaseDePartida.EsperandoAlRival -> CentradoPartida {
                    CircularProgressIndicator()
                    Text(
                        stringResource(R.string.partida_esperando_al_rival, marcador?.rival?.alias ?: ""),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                is FaseDePartida.Terminada -> ResultadoDeLaPartida(fase.marcador, alVolver)
                is FaseDePartida.Cerrada -> CentradoPartida {
                    Text(stringResource(fase.mensaje), textAlign = TextAlign.Center)
                    Button(onClick = alVolver, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.partida_volver)) }
                }
                is FaseDePartida.Error -> CentradoPartida {
                    Text(stringResource(fase.mensaje), textAlign = TextAlign.Center)
                    Button(onClick = viewModel::reintentar, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.partida_reintentar))
                    }
                }
            }
        }
    }

    if (confirmandoSalida) {
        AlertDialog(
            onDismissRequest = { confirmandoSalida = false },
            title = { Text(stringResource(R.string.partida_salir_titulo)) },
            text = { Text(stringResource(R.string.partida_salir_texto)) },
            confirmButton = {
                TextButton(onClick = { confirmandoSalida = false; alVolver() }) { Text(stringResource(R.string.partida_salir_confirmar)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoSalida = false }) { Text(stringResource(R.string.partida_salir_cancelar)) }
            },
        )
    }
}

// HU59-T3: tu puntaje contra el del rival, con los ejercicios respondidos de cada uno y si el rival tiene señal. Se anuncia a
// los lectores de pantalla cada vez que cambia.
@Composable
private fun MarcadorEnVivo(marcador: Marcador) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            JugadorDelMarcadorVista(stringResource(R.string.partida_tu_alias), marcador.yo, marcador.totalEjercicios, Alignment.Start)
            JugadorDelMarcadorVista(marcador.rival.alias, marcador.rival, marcador.totalEjercicios, Alignment.End)
        }
        Text(
            stringResource(if (marcador.rivalConectado) R.string.partida_rival_conectado else R.string.partida_rival_sin_senal, marcador.rival.alias),
            style = MaterialTheme.typography.bodySmall,
            color = if (marcador.rivalConectado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun JugadorDelMarcadorVista(nombre: String, jugador: JugadorDelMarcador, total: Int, alineacion: Alignment.Horizontal) {
    Column(horizontalAlignment = alineacion) {
        Text(nombre, style = MaterialTheme.typography.titleMedium)
        Text(jugador.puntaje.toString(), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.partida_respondidos, jugador.respondidos, total), style = MaterialTheme.typography.bodySmall)
    }
}

// El ejercicio del turno con las pantallas de ejercicio de siempre. Observa el estado del propio ejercicio para mostrar
// «Siguiente ejercicio» (o «Ver resultado» si la partida ya terminó) en cuanto está corregido.
@Composable
private fun EjercicioDeLaPartida(
    fase: FaseDePartida.Jugando,
    finDeLaPartida: Boolean,
    alSiguiente: () -> Unit,
    alVencerSesion: () -> Unit,
) {
    val ejercicio = fase.ejercicio
    val estadoDelEjercicio by ejercicio.uiState.collectAsStateWithLifecycle()
    val respondido = estadoDelEjercicio.envio is EstadoDeEnvio.Resultado

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.partida_turno, fase.turno, fase.total),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (fase.tipo) {
                TIPO_PREDICCION_SALIDA -> PredecirSalidaScreen(ejercicio, alVencerSesion)
                TIPO_COMPLETAR_ESPACIO -> CompletarEspacioScreen(ejercicio, alVencerSesion)
                TIPO_OPCION_MULTIPLE -> OpcionMultipleScreen(ejercicio, alVencerSesion)
                else -> CentradoPartida { Text(stringResource(R.string.partida_tipo_no_soportado), textAlign = TextAlign.Center) }
            }
        }
        if (respondido) {
            Button(
                onClick = alSiguiente,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            ) { Text(stringResource(if (finDeLaPartida) R.string.partida_ver_resultado else R.string.partida_siguiente)) }
        }
    }
}

// El final de la partida: quién ganó, por qué (si fue por abandono) y el marcador final.
@Composable
private fun ResultadoDeLaPartida(marcador: Marcador, alVolver: () -> Unit) {
    val texto = textoFinalDe(marcador)
    CentradoPartida {
        Text(
            stringResource(texto.titulo),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        texto.motivo?.let { Text(stringResource(it), textAlign = TextAlign.Center) }
        Text(stringResource(R.string.partida_final_puntaje, marcador.yo.puntaje, marcador.rival.puntaje), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.partida_titulo, marcador.rival.alias), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = alVolver, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.partida_volver)) }
    }
}

@Composable
private fun CentradoPartida(contenido: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { contenido() }
    }
}
