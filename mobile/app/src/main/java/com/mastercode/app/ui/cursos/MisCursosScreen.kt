package com.mastercode.app.ui.cursos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R
import com.mastercode.app.data.cursos.CursoInscrito
import com.mastercode.app.ui.componentes.BotonSecundario
import com.mastercode.app.ui.componentes.CabeceraMC
import com.mastercode.app.ui.componentes.EsqueletoDeTarjetas
import com.mastercode.app.ui.componentes.EstadoVacio
import com.mastercode.app.ui.componentes.EtiquetaMC
import com.mastercode.app.ui.componentes.LogoLenguaje
import com.mastercode.app.ui.componentes.MensajeDeError
import com.mastercode.app.ui.componentes.PantallaMC
import com.mastercode.app.ui.componentes.TarjetaMC
import com.mastercode.app.ui.theme.MC
import java.time.format.DateTimeFormatter
import com.mastercode.app.ui.componentes.entrada

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

// La pestaña «Cursos»: la lista de cursos del estudiante, cada uno con el logo de su lenguaje. El Dojo, las batallas y el perfil ya no
// se abren desde aquí: tienen su lugar en la barra inferior (pie).
@Composable
fun MisCursosScreen(
    viewModel: MisCursosViewModel,
    alAbrirCurso: (Long) -> Unit,
    alVencerSesion: () -> Unit,
    pie: @Composable () -> Unit = {},
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

    PantallaMC(
        cabecera = {
            CabeceraMC(
                titulo = stringResource(R.string.mis_cursos_titulo),
                sobretitulo = stringResource(R.string.cursos_sobretitulo),
            )
        },
        pie = pie,
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        ) {
            when (val contenido = uiState.contenido) {
                ContenidoMisCursos.Cargando, ContenidoMisCursos.SesionVencida -> item {
                    EsqueletoDeTarjetas(
                        descripcion = stringResource(R.string.mis_cursos_cargando),
                        cantidad = 3,
                        alto = 96.dp,
                    )
                }
                ContenidoMisCursos.Vacio -> item {
                    EstadoVacio(
                        ilustracion = R.drawable.ilu_vacio_cursos,
                        texto = stringResource(R.string.mis_cursos_vacio),
                    )
                }
                is ContenidoMisCursos.Error -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MensajeDeError(stringResource(contenido.mensaje))
                        BotonSecundario(
                            texto = stringResource(R.string.mis_cursos_reintentar),
                            alTocar = viewModel::cargar,
                            icono = Icons.Rounded.Refresh,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                is ContenidoMisCursos.ConCursos -> itemsIndexed(contenido.cursos, key = { _, curso -> curso.cursoId }) { indice, curso ->
                    // Las primeras tarjetas entran una tras otra; las que aparecen al desplazarse no se animan.
                    val entrada = if (indice < 6) Modifier.entrada(indice * 70) else Modifier
                    TarjetaDeCurso(curso, alTocar = { viewModel.alTocarCurso(curso.cursoId) }, modifier = entrada)
                }
            }
        }
    }
}

// Toda la tarjeta es tocable; los textos no tienen maxLines para que no se corten en pantallas pequeñas. A la izquierda, el logo del
// lenguaje en una caja con borde; a la derecha, el nombre del curso, el lenguaje y la fecha.
@Composable
private fun TarjetaDeCurso(curso: CursoInscrito, alTocar: () -> Unit, modifier: Modifier = Modifier) {
    val c = MC.colores
    TarjetaMC(modifier = modifier.fillMaxWidth(), alTocar = alTocar, etiquetaDeToque = stringResource(R.string.mis_cursos_abrir)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val forma = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(c.lienzo, forma)
                    .border(1.dp, c.borde, forma),
                contentAlignment = Alignment.Center,
            ) {
                LogoLenguaje(curso.lenguaje, 32.dp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EtiquetaMC(curso.lenguaje, color = c.acento, comentario = false)
                Text(curso.nombre, style = MaterialTheme.typography.titleMedium, color = c.texto)
                curso.fechaInscripcion?.let {
                    Text(
                        stringResource(R.string.mis_cursos_inscrito_el, it.format(FORMATO_FECHA)),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textoSuave,
                    )
                }
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textoSuave, modifier = Modifier.size(24.dp))
        }
    }
}
