package com.mastercode.app.ui.duelos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R
import com.mastercode.app.data.duelos.PuestoDelRanking

// HU60-T3: el ranking del curso elegido. La fila propia va resaltada; si todavía no se cargó su página, el puesto propio
// queda fijo abajo para verlo siempre.
@Composable
fun RankingTab(viewModel: RankingViewModel, alVencerSesion: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    AlVencerLaSesion(estado.sesionVencida, alVencerSesion)

    when (val cursos = estado.cursos) {
        ContenidoDeCursos.Cargando, ContenidoDeCursos.SesionVencida -> CargandoDuelos()
        ContenidoDeCursos.Vacio -> CentradoDuelos { Text(stringResource(R.string.duelos_sin_cursos), textAlign = TextAlign.Center) }
        is ContenidoDeCursos.Error -> ErrorConReintento(cursos.mensaje, viewModel::cargarCursos)
        is ContenidoDeCursos.Lista -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Con un solo curso ya viene elegido y no hace falta mostrar el selector.
            if (cursos.cursos.size > 1) SelectorDeCurso(cursos.cursos, estado.cursoElegido, viewModel::elegirCurso)
            when {
                estado.cursoElegido == null -> Text(stringResource(R.string.ranking_elegir_curso))
                estado.cargando -> CargandoDuelos()
                estado.error != null && estado.puestos.isEmpty() -> ErrorConReintento(estado.error!!, viewModel::recargar)
                estado.puestos.isEmpty() -> Text(stringResource(R.string.ranking_vacio))
                else -> {
                    // Hay ranking pero el estudiante todavía no jugó en este curso.
                    if (estado.sinPartidas) Text(stringResource(R.string.ranking_sin_partidas))
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                    ) {
                        items(estado.puestos, key = { it.posicion }) { puesto -> FilaDeRanking(puesto) }
                        item {
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
                                ) { Text(stringResource(R.string.ranking_ver_mas)) }
                            }
                        }
                    }
                    estado.miPuestoFueraDeLaLista?.let { mio ->
                        Text(stringResource(R.string.ranking_mi_puesto), style = MaterialTheme.typography.labelLarge)
                        FilaDeRanking(mio)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaDeRanking(puesto: PuestoDelRanking) {
    val colores = if (puesto.soyYo) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors()
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = colores) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val alias = if (puesto.soyYo) stringResource(R.string.ranking_yo, puesto.alias) else puesto.alias
            Text(stringResource(R.string.ranking_fila, puesto.posicion, alias), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.ranking_puntos, puesto.puntos, porcentajeTexto(puesto.porcentajeVictorias)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.ranking_detalle, puesto.jugadas, puesto.ganadas, puesto.empatadas, puesto.perdidas),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
