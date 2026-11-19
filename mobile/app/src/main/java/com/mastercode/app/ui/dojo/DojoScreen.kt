package com.mastercode.app.ui.dojo

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mastercode.app.R
import com.mastercode.app.data.problemas.ProblemaDelEjercicio
import com.mastercode.app.data.problemas.ProblemaRepository
import com.mastercode.app.data.respuestas.EnviadorDeRespuestas
import com.mastercode.app.ui.componentes.BotonDeTextoSobreHero
import com.mastercode.app.ui.componentes.BotonPrincipal
import com.mastercode.app.ui.componentes.BotonSecundario
import com.mastercode.app.ui.componentes.CabeceraMC
import com.mastercode.app.ui.componentes.FichaDeLenguaje
import com.mastercode.app.ui.componentes.MensajeDeError
import com.mastercode.app.ui.componentes.PantallaMC
import com.mastercode.app.ui.componentes.TarjetaMC
import com.mastercode.app.ui.ejercicios.CompletarEspacioScreen
import com.mastercode.app.ui.ejercicios.EjercicioViewModel
import com.mastercode.app.ui.ejercicios.EstadoDeEnvio
import com.mastercode.app.ui.ejercicios.OpcionMultipleScreen
import com.mastercode.app.ui.ejercicios.PredecirSalidaScreen
import com.mastercode.app.ui.ejercicios.TIPO_COMPLETAR_ESPACIO
import com.mastercode.app.ui.ejercicios.TIPO_OPCION_MULTIPLE
import com.mastercode.app.ui.ejercicios.TIPO_PREDICCION_SALIDA
import com.mastercode.app.ui.theme.BordeSobreHero
import com.mastercode.app.ui.theme.CristalSobreHero
import com.mastercode.app.ui.theme.FuegoDeRacha
import com.mastercode.app.ui.theme.MC
import com.mastercode.app.ui.theme.SobreHero

// HU53-T3 (CA-53.1, CA-53.4): la pantalla del Dojo. Primero se elige el lenguaje, lo que abre la sesión; luego va un
// ejercicio tras otro, con el contador de aciertos arriba y «Siguiente ejercicio» cuando el actual ya tiene respuesta.
// HU55-T1 (CA-55.1, CA-55.5): al salir de la pantalla (atrás, otra app o pantalla apagada) se guarda el estado.
// HU55-T2 (CA-55.2): si quedó una sesión guardada, se ofrece continuarla. HU55-T3 (CA-55.3, CA-55.4): «Terminar sesión»
// con confirmación. HU56-T2: al cerrar, la pantalla de resumen (ResumenDeSesionScreen).
@Composable
fun DojoScreen(
    viewModel: DojoViewModel,
    problemaRepository: ProblemaRepository,
    enviador: EnviadorDeRespuestas,
    alVolver: () -> Unit,
    alVencerSesion: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val c = MC.colores

    BackHandler {
        viewModel.alSalir()
        alVolver()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.alSalir() }
    DisposableEffect(viewModel) { onDispose { viewModel.alSalir() } }
    LaunchedEffect(uiState.fase) {
        if (uiState.fase == FaseDelDojo.SesionVencida) alVencerSesion()
    }

    val faseActual = uiState.fase
    if (faseActual is FaseDelDojo.Terminada) {
        // HU56-T2: el resumen es una pantalla completa con su propio encabezado, sin el del Dojo.
        ResumenDeSesionScreen(faseActual.resumen, alVolver)
    } else {
        PantallaMC(
            cabecera = {
                CabeceraMC(
                    titulo = stringResource(R.string.dojo_titulo),
                    sobretitulo = stringResource(R.string.dojo_sobretitulo),
                    alVolver = {
                        viewModel.alSalir()
                        alVolver()
                    },
                    extra = {
                        // El contador aparece cuando ya hay sesión y avisa a los lectores de pantalla cada vez que cambia.
                        if (uiState.sesionId != null || puedeTerminar(uiState)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (uiState.sesionId != null) {
                                    Row(
                                        modifier = Modifier
                                            .background(CristalSobreHero, CircleShape)
                                            .border(1.dp, BordeSobreHero, CircleShape)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .semantics { liveRegion = LiveRegionMode.Polite },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = FuegoDeRacha, modifier = Modifier.size(16.dp))
                                        Text(
                                            stringResource(R.string.dojo_aciertos, uiState.aciertos),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = SobreHero,
                                        )
                                    }
                                } else {
                                    Box {}
                                }
                                if (puedeTerminar(uiState)) {
                                    BotonDeTextoSobreHero(stringResource(R.string.dojo_terminar), viewModel::pedirTerminar)
                                }
                            }
                        }
                    },
                )
            },
        ) {
            uiState.errorAlTerminar?.let {
                MensajeDeError(
                    stringResource(it),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).semantics { liveRegion = LiveRegionMode.Polite },
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
                        Icon(Icons.Rounded.Flag, contentDescription = null, tint = c.acento, modifier = Modifier.size(40.dp))
                        Text(stringResource(R.string.dojo_sin_mas), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        BotonPrincipal(stringResource(R.string.dojo_volver), alVolver, modifier = Modifier.fillMaxWidth())
                    }
                    is FaseDelDojo.OfreciendoReanudar -> Centrado {
                        TarjetaMC(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(modifier = Modifier.size(52.dp).background(c.acentoSuave, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.History, contentDescription = null, tint = c.acento, modifier = Modifier.size(28.dp))
                                }
                                Text(
                                    stringResource(R.string.dojo_reanudar_titulo),
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.semantics { heading() },
                                )
                                Text(
                                    stringResource(
                                        R.string.dojo_reanudar_texto,
                                        stringResource(nombreDelLenguaje(fase.estado.lenguaje)),
                                        fase.estado.aciertos,
                                        fase.estado.respondidos,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = c.textoSuave,
                                    textAlign = TextAlign.Center,
                                )
                                fase.error?.let { MensajeDeError(stringResource(it)) }
                                BotonPrincipal(stringResource(R.string.dojo_reanudar_continuar), viewModel::reanudar, modifier = Modifier.fillMaxWidth())
                                BotonSecundario(stringResource(R.string.dojo_reanudar_nueva), viewModel::empezarDeNuevo, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    is FaseDelDojo.Terminada -> Unit
                    is FaseDelDojo.Error -> Centrado {
                        MensajeDeError(stringResource(fase.mensaje))
                        BotonSecundario(
                            texto = stringResource(R.string.dojo_reintentar),
                            alTocar = viewModel::alReintentar,
                            icono = Icons.Rounded.Refresh,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            if (uiState.fase is FaseDelDojo.Ejercicio && uiState.respondido) {
                // La acción siguiente queda fija abajo: el estudiante responde, lee la corrección y sigue con un toque.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.fondo)
                        .border(1.dp, c.borde)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    BotonPrincipal(stringResource(R.string.dojo_siguiente), viewModel::siguiente, modifier = Modifier.fillMaxWidth())
                }
            }
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

// CA-53.1: una ficha por lenguaje, con los colores que ese lenguaje tiene en toda la app.
@Composable
private fun ElegirLenguaje(alElegir: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            stringResource(R.string.dojo_elegir_lenguaje),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(R.string.dojo_elegir_descripcion), style = MaterialTheme.typography.bodyMedium, color = MC.colores.textoSuave)
        for (lenguaje in LENGUAJES_DEL_DOJO) {
            FichaDeLenguaje(lenguaje = lenguaje, nombre = stringResource(nombreDelLenguaje(lenguaje)), alTocar = { alElegir(lenguaje) })
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
    Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { contenido() }
    }
}
