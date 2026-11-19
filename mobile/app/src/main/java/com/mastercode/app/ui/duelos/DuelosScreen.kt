package com.mastercode.app.ui.duelos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mastercode.app.R
import com.mastercode.app.data.cursos.CursoInscrito
import com.mastercode.app.data.cursos.MisCursosRepository
import com.mastercode.app.data.duelos.DuelosRepository

private val PESTANAS = listOf(
    R.string.duelos_tab_retar,
    R.string.duelos_tab_invitaciones,
    R.string.duelos_tab_historial,
    R.string.duelos_tab_ranking,
)

// HU57-T3, H7 y HU60-T3: la sección Duelos, a la que se entra desde «Mis cursos». Cuatro pestañas: retar a un compañero,
// las invitaciones recibidas, el historial de partidas y el ranking de un curso. Retar y aceptar una invitación abren la
// partida (alAbrirPartida). Cada pestaña tiene su ViewModel, que se conserva al cambiar de pestaña.
@Composable
fun DuelosScreen(
    misCursos: MisCursosRepository,
    duelos: DuelosRepository,
    // Cambia en cada visita (y al volver de una partida): así las pestañas empiezan con ViewModels nuevos y datos frescos.
    visita: String,
    alAbrirPartida: (partidaId: Long) -> Unit,
    alVolver: () -> Unit,
    alVencerSesion: () -> Unit,
) {
    var pestana by rememberSaveable { mutableIntStateOf(0) }

    BackHandler(onBack = alVolver)

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.duelos_titulo), style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = alVolver, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.duelos_volver)) }
        }
        // Una fila que se desliza si no cabe: «Invitaciones» es larga en pantallas pequeñas.
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PESTANAS.forEachIndexed { indice, nombre ->
                val elegida = indice == pestana
                val modificador = Modifier.heightIn(min = 48.dp).semantics { selected = elegida }
                if (elegida) {
                    Button(onClick = { pestana = indice }, modifier = modificador) { Text(stringResource(nombre)) }
                } else {
                    OutlinedButton(onClick = { pestana = indice }, modifier = modificador) { Text(stringResource(nombre)) }
                }
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp)) {
            when (pestana) {
                0 -> RetarTab(
                    viewModel = viewModel(key = "duelos-retar-$visita", factory = RetarViewModel.factory(misCursos, duelos)),
                    alAbrirPartida = alAbrirPartida,
                    alVencerSesion = alVencerSesion,
                )
                1 -> InvitacionesTab(
                    viewModel = viewModel(key = "duelos-invitaciones-$visita", factory = InvitacionesViewModel.factory(duelos)),
                    alAbrirPartida = alAbrirPartida,
                    alVencerSesion = alVencerSesion,
                )
                2 -> HistorialTab(
                    viewModel = viewModel(key = "duelos-historial-$visita", factory = HistorialViewModel.factory(duelos)),
                    alAbrirPartida = alAbrirPartida,
                    alVencerSesion = alVencerSesion,
                )
                else -> RankingTab(
                    viewModel = viewModel(key = "duelos-ranking-$visita", factory = RankingViewModel.factory(misCursos, duelos)),
                    alVencerSesion = alVencerSesion,
                )
            }
        }
    }
}

// Lo que comparten las pestañas: el selector de curso, el centrado y el aviso cuando vence la sesión.

// Los cursos en una columna de botones: el elegido va relleno.
@Composable
internal fun SelectorDeCurso(cursos: List<CursoInscrito>, elegido: Long?, alElegir: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.duelos_elegir_curso), style = MaterialTheme.typography.titleMedium)
        for (curso in cursos) {
            val esElElegido = curso.cursoId == elegido
            val modificador = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { selected = esElElegido }
            if (esElElegido) {
                Button(onClick = { alElegir(curso.cursoId) }, modifier = modificador) { Text(curso.nombre) }
            } else {
                OutlinedButton(onClick = { alElegir(curso.cursoId) }, modifier = modificador) { Text(curso.nombre) }
            }
        }
    }
}

@Composable
internal fun CentradoDuelos(contenido: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { contenido() }
    }
}

@Composable
internal fun CargandoDuelos() = CentradoDuelos {
    val descripcion = stringResource(R.string.duelos_cargando)
    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = descripcion })
}

@Composable
internal fun ErrorConReintento(mensaje: Int, alReintentar: () -> Unit) = CentradoDuelos {
    Text(stringResource(mensaje), textAlign = TextAlign.Center)
    Button(onClick = alReintentar, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.duelos_reintentar)) }
}

// Cuando la sesión vence, la pestaña avisa una vez para volver al inicio de sesión.
@Composable
internal fun AlVencerLaSesion(vencida: Boolean, alVencerSesion: () -> Unit) {
    LaunchedEffect(vencida) { if (vencida) alVencerSesion() }
}
