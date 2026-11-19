package com.mastercode.app.ui.duelos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mastercode.app.R
import com.mastercode.app.data.cursos.CursoInscrito
import com.mastercode.app.data.cursos.MisCursosRepository
import com.mastercode.app.data.duelos.DuelosRepository
import com.mastercode.app.ui.componentes.BotonSecundario
import com.mastercode.app.ui.componentes.CabeceraMC
import com.mastercode.app.ui.componentes.EsqueletoDeTarjetas
import com.mastercode.app.ui.componentes.MensajeDeError
import com.mastercode.app.ui.componentes.PantallaMC
import com.mastercode.app.ui.componentes.PestanasMC
import com.mastercode.app.ui.theme.MC

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

    PantallaMC(
        cabecera = {
            CabeceraMC(
                titulo = stringResource(R.string.duelos_titulo),
                sobretitulo = stringResource(R.string.duelos_sobretitulo),
                alVolver = alVolver,
                extra = {
                    // Los dos contrincantes: pura decoración, el título ya dice de qué va la pantalla.
                    Image(
                        painter = painterResource(R.drawable.ilu_duelos_banner),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.padding(top = 14.dp).fillMaxWidth().height(96.dp).clip(RoundedCornerShape(8.dp)),
                    )
                },
            )
        },
    ) {
        // Una fila de píldoras que se desliza si no cabe: «Invitaciones» es larga en pantallas pequeñas.
        val nombres = PESTANAS.map { stringResource(it) }
        PestanasMC(
            opciones = nombres,
            elegida = pestana,
            alElegir = { pestana = it },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp)) {
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

// Los cursos como píldoras: el elegido va relleno.
@Composable
internal fun SelectorDeCurso(cursos: List<CursoInscrito>, elegido: Long?, alElegir: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.duelos_elegir_curso), style = MaterialTheme.typography.titleSmall, color = MC.colores.textoSuave)
        PestanasMC(
            opciones = cursos.map { it.nombre },
            elegida = cursos.indexOfFirst { it.cursoId == elegido },
            alElegir = { alElegir(cursos[it].cursoId) },
            rol = Role.RadioButton,
        )
    }
}

@Composable
internal fun CentradoDuelos(contenido: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { contenido() }
    }
}

@Composable
internal fun CargandoDuelos() {
    EsqueletoDeTarjetas(descripcion = stringResource(R.string.duelos_cargando), cantidad = 3, alto = 84.dp)
}

@Composable
internal fun ErrorConReintento(mensaje: Int, alReintentar: () -> Unit) = CentradoDuelos {
    MensajeDeError(stringResource(mensaje))
    BotonSecundario(
        texto = stringResource(R.string.duelos_reintentar),
        alTocar = alReintentar,
        icono = Icons.Rounded.Refresh,
        modifier = Modifier.fillMaxWidth(),
    )
}

// Cuando la sesión vence, la pestaña avisa una vez para volver al inicio de sesión.
@Composable
internal fun AlVencerLaSesion(vencida: Boolean, alVencerSesion: () -> Unit) {
    LaunchedEffect(vencida) { if (vencida) alVencerSesion() }
}
