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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R
import com.mastercode.app.ui.componentes.AvatarMC
import com.mastercode.app.ui.componentes.BotonPrincipal
import com.mastercode.app.ui.componentes.EstadoVacio
import com.mastercode.app.ui.componentes.MensajeDeError
import com.mastercode.app.ui.componentes.TarjetaMC

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
        ContenidoDeCursos.Vacio -> EstadoVacio(ilustracion = R.drawable.ilu_vacio_cursos, texto = stringResource(R.string.duelos_sin_cursos))
        is ContenidoDeCursos.Error -> ErrorConReintento(cursos.mensaje, viewModel::cargarCursos)
        is ContenidoDeCursos.Lista -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Con un solo curso ya viene elegido y no hace falta mostrar el selector.
            if (cursos.cursos.size > 1) SelectorDeCurso(cursos.cursos, estado.cursoElegido, viewModel::elegirCurso)
            estado.error?.let {
                MensajeDeError(stringResource(it), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            when (val companeros = estado.companeros) {
                ContenidoDeCompaneros.SinCurso -> Unit
                ContenidoDeCompaneros.Cargando -> CargandoDuelos()
                ContenidoDeCompaneros.Vacio -> EstadoVacio(ilustracion = R.drawable.ilu_vacio_invitaciones, texto = stringResource(R.string.retar_sin_companeros))
                is ContenidoDeCompaneros.Error -> ErrorConReintento(companeros.mensaje) {
                    estado.cursoElegido?.let(viewModel::elegirCurso)
                }
                is ContenidoDeCompaneros.Lista -> {
                    Text(
                        stringResource(R.string.retar_elegir_companero),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                        items(companeros.companeros, key = { it.id }) { companero ->
                            TarjetaMC(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    AvatarMC(companero.alias)
                                    Text(companero.alias, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                    if (estado.retando == companero.id) {
                                        val descripcion = stringResource(R.string.retar_enviando)
                                        CircularProgressIndicator(modifier = Modifier.size(28.dp).semantics { contentDescription = descripcion })
                                    } else {
                                        // El botón dice «Retar»; el lector de pantalla dice a quién.
                                        val completo = stringResource(R.string.retar_boton, companero.alias)
                                        BotonPrincipal(
                                            texto = stringResource(R.string.retar_corto),
                                            alTocar = { viewModel.retar(companero) },
                                            habilitado = estado.retando == null,
                                            compacto = true,
                                            modifier = Modifier.semantics { contentDescription = completo },
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
}
