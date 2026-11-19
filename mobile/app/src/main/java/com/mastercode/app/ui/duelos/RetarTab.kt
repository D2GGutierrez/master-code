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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R

// HU57-T3: elegir el curso, ver a los compañeros inscritos y retar a uno. Al crearse el reto se abre su partida, donde el
// retador espera a que el rival acepte.
@Composable
fun RetarTab(viewModel: RetarViewModel, alAbrirPartida: (Long) -> Unit, alVencerSesion: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    AlVencerLaSesion(estado.sesionVencida, alVencerSesion)
    LaunchedEffect(estado.retoEnviado) {
        estado.retoEnviado?.let {
            alAbrirPartida(it.id)
            viewModel.alAbrirseLaPartida()
        }
    }

    when (val cursos = estado.cursos) {
        ContenidoDeCursos.Cargando, ContenidoDeCursos.SesionVencida -> CargandoDuelos()
        ContenidoDeCursos.Vacio -> CentradoDuelos { Text(stringResource(R.string.duelos_sin_cursos), textAlign = TextAlign.Center) }
        is ContenidoDeCursos.Error -> ErrorConReintento(cursos.mensaje, viewModel::cargarCursos)
        is ContenidoDeCursos.Lista -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Con un solo curso ya viene elegido y no hace falta mostrar el selector.
            if (cursos.cursos.size > 1) SelectorDeCurso(cursos.cursos, estado.cursoElegido, viewModel::elegirCurso)
            estado.error?.let {
                Text(
                    stringResource(it),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            when (val companeros = estado.companeros) {
                ContenidoDeCompaneros.SinCurso -> Unit
                ContenidoDeCompaneros.Cargando -> CargandoDuelos()
                ContenidoDeCompaneros.Vacio -> Text(stringResource(R.string.retar_sin_companeros))
                is ContenidoDeCompaneros.Error -> ErrorConReintento(companeros.mensaje) {
                    estado.cursoElegido?.let(viewModel::elegirCurso)
                }
                is ContenidoDeCompaneros.Lista -> {
                    Text(stringResource(R.string.retar_elegir_companero), style = MaterialTheme.typography.titleMedium)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(companeros.companeros, key = { it.id }) { companero ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(companero.alias, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                    if (estado.retando == companero.id) {
                                        val descripcion = stringResource(R.string.retar_enviando)
                                        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = descripcion })
                                    } else {
                                        Button(
                                            onClick = { viewModel.retar(companero) },
                                            enabled = estado.retando == null,
                                            modifier = Modifier.heightIn(min = 48.dp),
                                        ) { Text(stringResource(R.string.retar_boton, companero.alias)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
