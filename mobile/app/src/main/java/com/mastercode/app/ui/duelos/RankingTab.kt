package com.mastercode.app.ui.duelos

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R
import com.mastercode.app.data.duelos.PuestoDelRanking
import com.mastercode.app.ui.componentes.BotonSecundario
import com.mastercode.app.ui.componentes.EstadoVacio
import com.mastercode.app.ui.componentes.MensajeDeError
import com.mastercode.app.ui.componentes.TarjetaMC
import com.mastercode.app.ui.theme.Bronce
import com.mastercode.app.ui.theme.MC
import com.mastercode.app.ui.theme.Oro
import com.mastercode.app.ui.theme.Plata

// HU60-T3: el ranking del curso elegido. La fila propia va resaltada; si todavía no se cargó su página, el puesto propio
// queda fijo abajo para verlo siempre.
@Composable
fun RankingTab(viewModel: RankingViewModel, alVencerSesion: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    AlVencerLaSesion(estado.sesionVencida, alVencerSesion)

    when (val cursos = estado.cursos) {
        ContenidoDeCursos.Cargando, ContenidoDeCursos.SesionVencida -> CargandoDuelos()
        ContenidoDeCursos.Vacio -> EstadoVacio(ilustracion = R.drawable.ilu_vacio_cursos, texto = stringResource(R.string.duelos_sin_cursos))
        is ContenidoDeCursos.Error -> ErrorConReintento(cursos.mensaje, viewModel::cargarCursos)
        is ContenidoDeCursos.Lista -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Con un solo curso ya viene elegido y no hace falta mostrar el selector.
            if (cursos.cursos.size > 1) SelectorDeCurso(cursos.cursos, estado.cursoElegido, viewModel::elegirCurso)
            when {
                estado.cursoElegido == null -> Text(stringResource(R.string.ranking_elegir_curso), style = MaterialTheme.typography.bodyMedium, color = MC.colores.textoSuave)
                estado.cargando -> CargandoDuelos()
                estado.error != null && estado.puestos.isEmpty() -> ErrorConReintento(estado.error!!, viewModel::recargar)
                estado.puestos.isEmpty() -> EstadoVacio(ilustracion = R.drawable.ilu_vacio_ranking, texto = stringResource(R.string.ranking_vacio))
                else -> {
                    // Hay ranking pero el estudiante todavía no jugó en este curso.
                    if (estado.sinPartidas) {
                        Text(stringResource(R.string.ranking_sin_partidas), style = MaterialTheme.typography.bodyMedium, color = MC.colores.textoSuave)
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                    ) {
                        items(estado.puestos, key = { it.posicion }) { puesto -> FilaDeRanking(puesto) }
                        item {
                            estado.error?.let {
                                MensajeDeError(stringResource(it), modifier = Modifier.padding(vertical = 8.dp))
                            }
                            if (estado.cargandoMas) {
                                val descripcion = stringResource(R.string.duelos_cargando)
                                CircularProgressIndicator(modifier = Modifier.size(28.dp).semantics { contentDescription = descripcion })
                            } else if (estado.hayMas) {
                                BotonSecundario(
                                    texto = stringResource(R.string.ranking_ver_mas),
                                    alTocar = viewModel::cargarMas,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    estado.miPuestoFueraDeLaLista?.let { mio ->
                        Text(
                            stringResource(R.string.ranking_mi_puesto),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.semantics { heading() },
                        )
                        FilaDeRanking(mio)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaDeRanking(puesto: PuestoDelRanking) {
    val c = MC.colores
    TarjetaMC(modifier = Modifier.fillMaxWidth(), resaltada = puesto.soyYo) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InsigniaDePuesto(puesto.posicion)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val alias = if (puesto.soyYo) stringResource(R.string.ranking_yo, puesto.alias) else puesto.alias
                Text(alias, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.ranking_puntos, puesto.puntos, porcentajeTexto(puesto.porcentajeVictorias)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.ranking_detalle, puesto.jugadas, puesto.ganadas, puesto.empatadas, puesto.perdidas),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textoSuave,
                )
            }
        }
    }
}

// El número del puesto en un círculo: oro, plata y bronce para el podio, y neutro para los demás.
@Composable
private fun InsigniaDePuesto(posicion: Int) {
    val c = MC.colores
    val medalla: Color? = when (posicion) {
        1 -> Oro
        2 -> Plata
        3 -> Bronce
        else -> null
    }
    val posicionEnTexto = stringResource(R.string.ranking_posicion, posicion)
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(medalla ?: c.fondoSuave, CircleShape)
            .semantics { contentDescription = posicionEnTexto },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            posicion.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = if (medalla != null) Color(0xFF1B1747) else c.textoSuave,
        )
    }
}
