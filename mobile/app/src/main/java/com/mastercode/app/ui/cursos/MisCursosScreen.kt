package com.mastercode.app.ui.cursos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R
import com.mastercode.app.data.cursos.CursoInscrito
import java.time.format.DateTimeFormatter

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun MisCursosScreen(
    viewModel: MisCursosViewModel,
    alAbrirCurso: (Long) -> Unit,
    alVencerSesion: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.cursoAbierto) {
        uiState.cursoAbierto?.let {
            alAbrirCurso(it)
            viewModel.alCursoAbierto()
        }
    }
    LaunchedEffect(uiState.contenido) {
        if (uiState.contenido == ContenidoMisCursos.SesionVencida) alVencerSesion()
    }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp)) {
        Text(
            stringResource(R.string.mis_cursos_titulo),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        when (val contenido = uiState.contenido) {
            ContenidoMisCursos.Cargando, ContenidoMisCursos.SesionVencida -> Centrado {
                val descripcion = stringResource(R.string.mis_cursos_cargando)
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = descripcion })
            }
            ContenidoMisCursos.Vacio -> Centrado {
                Text(stringResource(R.string.mis_cursos_vacio), textAlign = TextAlign.Center)
            }
            is ContenidoMisCursos.Error -> Centrado {
                Text(stringResource(contenido.mensaje), textAlign = TextAlign.Center)
                Button(onClick = viewModel::cargar) {
                    Text(stringResource(R.string.mis_cursos_reintentar))
                }
            }
            is ContenidoMisCursos.ConCursos -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(contenido.cursos, key = { it.cursoId }) { curso ->
                    TarjetaCurso(curso, alTocar = { viewModel.alTocarCurso(curso.cursoId) })
                }
            }
        }
    }
}

@Composable
private fun Centrado(contenido: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { contenido() }
    }
}

// Toda la tarjeta es tocable; los textos no tienen maxLines para que no se corten en pantallas pequeñas.
@Composable
private fun TarjetaCurso(curso: CursoInscrito, alTocar: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.mis_cursos_abrir), onClick = alTocar),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(curso.nombre, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.mis_cursos_lenguaje, curso.lenguaje))
            curso.fechaInscripcion?.let {
                Text(
                    stringResource(R.string.mis_cursos_inscrito_el, it.format(FORMATO_FECHA)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
