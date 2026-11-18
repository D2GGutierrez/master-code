package com.mastercode.app.ui.dojo

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mastercode.app.R
import com.mastercode.app.data.dojo.ResumenDeSesion
import com.mastercode.app.data.problemas.ProblemaDelEjercicio
import com.mastercode.app.data.problemas.ProblemaRepository
import com.mastercode.app.data.respuestas.EnviadorDeRespuestas
import com.mastercode.app.ui.ejercicios.CompletarEspacioScreen
import com.mastercode.app.ui.ejercicios.EjercicioViewModel
import com.mastercode.app.ui.ejercicios.EstadoDeEnvio
import com.mastercode.app.ui.ejercicios.OpcionMultipleScreen
import com.mastercode.app.ui.ejercicios.PredecirSalidaScreen
import com.mastercode.app.ui.ejercicios.TIPO_COMPLETAR_ESPACIO
import com.mastercode.app.ui.ejercicios.TIPO_OPCION_MULTIPLE
import com.mastercode.app.ui.ejercicios.TIPO_PREDICCION_SALIDA

// HU53-T3 (CA-53.1, CA-53.4): la pantalla del Dojo. Primero se elige el lenguaje, lo que abre la sesión; luego va un
// ejercicio tras otro, con el contador de aciertos arriba y «Siguiente ejercicio» cuando el actual ya tiene respuesta.
// HU55-T1 (CA-55.1, CA-55.5): al salir de la pantalla (atrás, otra app o pantalla apagada) se guarda el estado.
// HU55-T2 (CA-55.2): si quedó una sesión guardada, se ofrece continuarla. HU55-T3 (CA-55.3, CA-55.4): «Terminar sesión»
// con confirmación y un resumen mínimo al cerrar.
@Composable
fun DojoScreen(
    viewModel: DojoViewModel,
    problemaRepository: ProblemaRepository,
    enviador: EnviadorDeRespuestas,
    alVolver: () -> Unit,
    alVencerSesion: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler {
        viewModel.alSalir()
        alVolver()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.alSalir() }
    DisposableEffect(viewModel) { onDispose { viewModel.alSalir() } }
    LaunchedEffect(uiState.fase) {
        if (uiState.fase == FaseDelDojo.SesionVencida) alVencerSesion()
    }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.dojo_titulo), style = MaterialTheme.typography.headlineMedium)
            // El contador aparece cuando ya hay sesión y avisa a los lectores de pantalla cada vez que cambia.
            if (uiState.sesionId != null) {
                Text(
                    stringResource(R.string.dojo_aciertos, uiState.aciertos),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
        if (puedeTerminar(uiState)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = viewModel::pedirTerminar,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.dojo_terminar)) }
            }
        }
        uiState.errorAlTerminar?.let {
            Text(
                stringResource(it),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val fase = uiState.fase) {
                FaseDelDojo.Eligiendo -> ElegirLenguaje(viewModel::elegirLenguaje)
                FaseDelDojo.Iniciando, FaseDelDojo.CargandoEjercicio, FaseDelDojo.SesionVencida, FaseDelDojo.Terminando -> Centrado {
                    val descripcion = stringResource(R.string.dojo_iniciando)
                    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = descripcion })
                }
                is FaseDelDojo.Ejercicio -> EjercicioDelDojo(
                    problema = fase.problema,
                    sesionId = uiState.sesionId,
                    problemaRepository = problemaRepository,
                    enviador = enviador,
                    alResponder = viewModel::alResponder,
                    alOmitir = viewModel::omitir,
                    alVencerSesion = alVencerSesion,
                )
                FaseDelDojo.SinMasEjercicios -> Centrado {
                    Text(stringResource(R.string.dojo_sin_mas), textAlign = TextAlign.Center)
                    Button(onClick = alVolver) { Text(stringResource(R.string.dojo_volver)) }
                }
                is FaseDelDojo.OfreciendoReanudar -> Centrado {
                    Text(stringResource(R.string.dojo_reanudar_titulo), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(
                            R.string.dojo_reanudar_texto,
                            stringResource(nombreDelLenguaje(fase.estado.lenguaje)),
                            fase.estado.aciertos,
                            fase.estado.respondidos,
                        ),
                        textAlign = TextAlign.Center,
                    )
                    fase.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
                    Button(
                        onClick = viewModel::reanudar,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.dojo_reanudar_continuar)) }
                    TextButton(
                        onClick = viewModel::empezarDeNuevo,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.dojo_reanudar_nueva)) }
                }
                is FaseDelDojo.Terminada -> ResumenDeLaSesion(fase.resumen, alVolver)
                is FaseDelDojo.Error -> Centrado {
                    Text(stringResource(fase.mensaje), textAlign = TextAlign.Center)
                    Button(onClick = viewModel::alReintentar) { Text(stringResource(R.string.dojo_reintentar)) }
                }
            }
        }
        if (uiState.fase is FaseDelDojo.Ejercicio && uiState.respondido) {
            Button(
                onClick = viewModel::siguiente,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            ) { Text(stringResource(R.string.dojo_siguiente)) }
        }
    }

    if (uiState.confirmandoTermino) {
        AlertDialog(
            onDismissRequest = viewModel::cancelarTerminar,
            title = { Text(stringResource(R.string.dojo_terminar_titulo)) },
            text = { Text(stringResource(R.string.dojo_terminar_texto)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmarTerminar) { Text(stringResource(R.string.dojo_terminar_confirmar)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarTerminar) { Text(stringResource(R.string.dojo_terminar_cancelar)) }
            },
        )
    }
}

// «Terminar sesión» solo con la sesión quieta: con ejercicio, sin más ejercicios o tras un error.
private fun puedeTerminar(estado: DojoUiState): Boolean =
    estado.sesionId != null &&
        (estado.fase is FaseDelDojo.Ejercicio || estado.fase == FaseDelDojo.SinMasEjercicios || estado.fase is FaseDelDojo.Error)

// HU55-T3 (CA-55.4): un resumen mínimo (aciertos, respondidos y puntos). El resumen completo es de HU56-T2.
@Composable
private fun ResumenDeLaSesion(resumen: ResumenDeSesion, alVolver: () -> Unit) {
    AlertDialog(
        onDismissRequest = alVolver,
        title = { Text(stringResource(R.string.dojo_resumen_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.dojo_resumen_aciertos, resumen.aciertos, resumen.respondidos))
                Text(stringResource(R.string.dojo_resumen_puntos, resumen.puntosSumados))
            }
        },
        confirmButton = { TextButton(onClick = alVolver) { Text(stringResource(R.string.dojo_volver)) } },
    )
}

@Composable
private fun ElegirLenguaje(alElegir: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.dojo_elegir_lenguaje), style = MaterialTheme.typography.titleMedium)
        for (lenguaje in LENGUAJES_DEL_DOJO) {
            Button(
                onClick = { alElegir(lenguaje) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text(stringResource(nombreDelLenguaje(lenguaje))) }
        }
    }
}

@StringRes
private fun nombreDelLenguaje(lenguaje: String): Int = when (lenguaje) {
    "python" -> R.string.dojo_lenguaje_python
    "java" -> R.string.dojo_lenguaje_java
    else -> R.string.dojo_lenguaje_javascript
}

// Un ejercicio de la sesión con su propio EjercicioViewModel (uno por ejercicio, ya cargado: el Dojo lo trajo). Cuando su
// respuesta queda corregida, el Dojo cuenta el acierto; si el ejercicio es de un tipo que el celular no muestra, se ofrece
// seguir sin contarlo.
@Composable
private fun EjercicioDelDojo(
    problema: ProblemaDelEjercicio,
    sesionId: Long?,
    problemaRepository: ProblemaRepository,
    enviador: EnviadorDeRespuestas,
    alResponder: (problemaId: Long, acerto: Boolean) -> Unit,
    alOmitir: () -> Unit,
    alVencerSesion: () -> Unit,
) {
    val viewModel: EjercicioViewModel = viewModel(
        key = "dojo-ejercicio-${problema.id}",
        factory = EjercicioViewModel.factory(
            problemaRepository, enviador, problema.id, problema.tipoEjercicio, sesionId, precargado = problema,
        ),
    )
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    // El Dojo ignora el aviso repetido (por ejemplo, al girar el teléfono), así que no suma dos veces.
    val envio = estado.envio
    if (envio is EstadoDeEnvio.Resultado) {
        LaunchedEffect(problema.id) { alResponder(problema.id, envio.acerto) }
    }

    when (problema.tipoEjercicio) {
        TIPO_PREDICCION_SALIDA -> PredecirSalidaScreen(viewModel, alVencerSesion)
        TIPO_COMPLETAR_ESPACIO -> CompletarEspacioScreen(viewModel, alVencerSesion)
        TIPO_OPCION_MULTIPLE -> OpcionMultipleScreen(viewModel, alVencerSesion)
        else -> {
            LaunchedEffect(problema.id) { alOmitir() }
            Centrado { Text(stringResource(R.string.dojo_tipo_no_soportado), textAlign = TextAlign.Center) }
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
