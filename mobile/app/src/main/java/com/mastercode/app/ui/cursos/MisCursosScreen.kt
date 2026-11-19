package com.mastercode.app.ui.cursos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R
import com.mastercode.app.data.cursos.CursoInscrito
import com.mastercode.app.ui.componentes.BotonSecundario
import com.mastercode.app.ui.componentes.EsqueletoDeTarjetas
import com.mastercode.app.ui.componentes.EstadoVacio
import com.mastercode.app.ui.componentes.HeroMC
import com.mastercode.app.ui.componentes.LogoLenguaje
import com.mastercode.app.ui.componentes.MensajeDeError
import com.mastercode.app.ui.componentes.TarjetaMC
import com.mastercode.app.ui.theme.MC
import com.mastercode.app.ui.theme.SobreHero
import com.mastercode.app.ui.theme.SobreHeroSuave
import java.time.format.DateTimeFormatter

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

// La pantalla principal: el hero con la racha, las cuatro puertas (Dojo, Duelos, Perfil y Avisos) y los cursos con su portada.
@Composable
fun MisCursosScreen(
    viewModel: MisCursosViewModel,
    alAbrirCurso: (Long) -> Unit,
    alAbrirDojo: () -> Unit,
    alVencerSesion: () -> Unit,
    alAbrirAvisos: () -> Unit = {},
    alAbrirPerfil: () -> Unit = {},
    // HU57-T3: la puerta a los duelos con compañeros.
    alAbrirDuelos: () -> Unit = {},
    // HU49-T3: lo que va bajo el título (la racha). Se pasa desde fuera para que esta pantalla no conozca la racha.
    encabezado: @Composable () -> Unit = {},
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MC.colores.lienzo)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            HeroMC {
                Text(stringResource(R.string.inicio_sobretitulo).uppercase(), style = MaterialTheme.typography.labelSmall, color = SobreHeroSuave)
                Text(
                    stringResource(R.string.mis_cursos_titulo),
                    style = MaterialTheme.typography.headlineLarge,
                    color = SobreHero,
                    modifier = Modifier.semantics { heading() },
                )
                encabezado()
            }
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.inicio_entrenar),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 6.dp).semantics { heading() },
                )
                // HU53-T3: la puerta al Dojo.
                AccionDestacada(Icons.Rounded.Bolt, stringResource(R.string.mis_cursos_dojo), stringResource(R.string.dojo_descripcion), alAbrirDojo)
                // HU57-T3: retar a un compañero, invitaciones, historial y ranking.
                AccionNormal(Icons.Rounded.SportsEsports, stringResource(R.string.mis_cursos_duelos), stringResource(R.string.duelos_descripcion), alAbrirDuelos)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // HU51-T2: el perfil con el cinturón y su barra de avance.
                    AccionPequena(Icons.Rounded.MilitaryTech, stringResource(R.string.mis_cursos_perfil), alAbrirPerfil, Modifier.weight(1f))
                    // HU52-T3: los avisos de práctica (activarlos y elegir la hora).
                    AccionPequena(Icons.Rounded.Notifications, stringResource(R.string.mis_cursos_avisos), alAbrirAvisos, Modifier.weight(1f))
                }
                Text(
                    stringResource(R.string.inicio_tus_cursos),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 10.dp).semantics { heading() },
                )
            }
        }
        when (val contenido = uiState.contenido) {
            ContenidoMisCursos.Cargando, ContenidoMisCursos.SesionVencida -> item {
                EsqueletoDeTarjetas(
                    descripcion = stringResource(R.string.mis_cursos_cargando),
                    modifier = Modifier.padding(horizontal = 20.dp),
                    cantidad = 2,
                    alto = 188.dp,
                )
            }
            ContenidoMisCursos.Vacio -> item {
                EstadoVacio(
                    ilustracion = R.drawable.ilu_vacio_cursos,
                    texto = stringResource(R.string.mis_cursos_vacio),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            is ContenidoMisCursos.Error -> item {
                Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MensajeDeError(stringResource(contenido.mensaje))
                    BotonSecundario(
                        texto = stringResource(R.string.mis_cursos_reintentar),
                        alTocar = viewModel::cargar,
                        icono = Icons.Rounded.Refresh,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            is ContenidoMisCursos.ConCursos -> items(contenido.cursos, key = { it.cursoId }) { curso ->
                TarjetaDeCurso(
                    curso,
                    alTocar = { viewModel.alTocarCurso(curso.cursoId) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }
}

// Las acciones de la pantalla principal son cajas con borde fino: un ícono gris, el título, la descripción y una flecha.
@Composable
private fun AccionDestacada(icono: ImageVector, titulo: String, descripcion: String, alTocar: () -> Unit) =
    AccionNormal(icono, titulo, descripcion, alTocar)

@Composable
private fun AccionNormal(icono: ImageVector, titulo: String, descripcion: String, alTocar: () -> Unit) {
    val c = MC.colores
    TarjetaMC(modifier = Modifier.fillMaxWidth(), alTocar = alTocar) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(icono, contentDescription = null, tint = c.textoSuave, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, color = c.acento)
                Text(descripcion, style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textoSuave, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun AccionPequena(icono: ImageVector, titulo: String, alTocar: () -> Unit, modifier: Modifier = Modifier) {
    val c = MC.colores
    TarjetaMC(modifier = modifier, alTocar = alTocar) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icono, contentDescription = null, tint = c.textoSuave, modifier = Modifier.size(24.dp))
            Text(titulo, style = MaterialTheme.typography.titleSmall, color = c.acento)
        }
    }
}

// Toda la tarjeta es tocable; los textos no tienen maxLines para que no se corten en pantallas pequeñas. Como un repositorio de
// GitHub: el nombre en azul, el logo del lenguaje y la fecha en gris.
@Composable
private fun TarjetaDeCurso(curso: CursoInscrito, alTocar: () -> Unit, modifier: Modifier = Modifier) {
    val c = MC.colores
    TarjetaMC(modifier = modifier.fillMaxWidth(), alTocar = alTocar, etiquetaDeToque = stringResource(R.string.mis_cursos_abrir)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(curso.nombre, style = MaterialTheme.typography.titleMedium, color = c.acento)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LogoLenguaje(curso.lenguaje, 18.dp)
                Text(stringResource(R.string.mis_cursos_lenguaje, curso.lenguaje), style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
            }
            curso.fechaInscripcion?.let {
                Text(
                    stringResource(R.string.mis_cursos_inscrito_el, it.format(FORMATO_FECHA)),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textoSuave,
                )
            }
        }
    }
}
