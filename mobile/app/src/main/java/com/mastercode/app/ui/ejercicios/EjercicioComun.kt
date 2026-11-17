package com.mastercode.app.ui.ejercicios

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mastercode.app.R
import com.mastercode.app.data.problemas.ProblemaDelEjercicio

// Lo que las pantallas de ejercicio comparten: el título, la carga, los errores y el envío de la respuesta (botón,
// resultado e intentos que quedan). Solo el contenido «Listo» cambia.
@Composable
fun MarcoDeEjercicio(
    uiState: EjercicioUiState,
    alReintentar: () -> Unit,
    alVencerSesion: () -> Unit,
    alEnviar: () -> Unit,
    alIntentarDeNuevo: () -> Unit,
    contenidoListo: @Composable (problema: ProblemaDelEjercicio) -> Unit,
) {
    LaunchedEffect(uiState.contenido) {
        if (uiState.contenido == ContenidoEjercicio.SesionVencida) alVencerSesion()
    }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp)) {
        when (val contenido = uiState.contenido) {
            ContenidoEjercicio.Cargando, ContenidoEjercicio.SesionVencida -> Centrado {
                val descripcion = stringResource(R.string.ejercicio_cargando)
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = descripcion })
            }
            is ContenidoEjercicio.Error -> Centrado {
                Text(stringResource(contenido.mensaje), textAlign = TextAlign.Center)
                Button(onClick = alReintentar) { Text(stringResource(R.string.ejercicio_reintentar)) }
            }
            is ContenidoEjercicio.Listo -> Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    contenido.problema.titulo,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
                contenidoListo(contenido.problema)
                PanelDeEnvio(uiState, alEnviar, alIntentarDeNuevo)
            }
        }
    }
}

// HU45-T3, HU48-T2, HU48-T3: el botón para enviar y, debajo, lo que pasó con la respuesta: si acertó o cuál era la
// correcta, que quedó guardada por falta de conexión, o por qué no se pudo enviar; siempre con los intentos que quedan.
@Composable
private fun PanelDeEnvio(uiState: EjercicioUiState, alEnviar: () -> Unit, alIntentarDeNuevo: () -> Unit) {
    when (val envio = uiState.envio) {
        EstadoDeEnvio.SinEnviar, is EstadoDeEnvio.Rechazado -> {
            if (envio is EstadoDeEnvio.Rechazado) {
                Text(stringResource(envio.mensaje), color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = alEnviar,
                enabled = uiState.puedeEnviar,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.ejercicio_enviar)) }
        }
        EstadoDeEnvio.Enviando -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val descripcion = stringResource(R.string.ejercicio_enviando)
            CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = descripcion })
        }
        EstadoDeEnvio.Pendiente -> Text(
            stringResource(R.string.ejercicio_pendiente),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        is EstadoDeEnvio.Resultado -> Column(
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(if (envio.acerto) R.string.ejercicio_acerto else R.string.ejercicio_fallo),
                style = MaterialTheme.typography.titleLarge,
                color = if (envio.acerto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            if (!envio.acerto && envio.respuestaCorrecta != null) {
                Text(stringResource(R.string.ejercicio_respuesta_correcta, envio.respuestaCorrecta))
            }
        }
    }
    uiState.intentosRestantes?.let { restantes ->
        Text(textoDeIntentos(restantes), style = MaterialTheme.typography.bodyMedium)
    }
    if (uiState.puedeIntentarDeNuevo) {
        OutlinedButton(onClick = alIntentarDeNuevo, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.ejercicio_intentar_de_nuevo))
        }
    }
}

@Composable
private fun textoDeIntentos(restantes: Int): String = when {
    restantes <= 0 -> stringResource(R.string.ejercicio_intentos_ninguno)
    restantes == 1 -> stringResource(R.string.ejercicio_intentos_uno)
    else -> stringResource(R.string.ejercicio_intentos_varios, restantes)
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

// Un fragmento de código: letra de ancho fijo y fondo distinto; si una línea no cabe, se desliza de lado en vez de cortarse.
@Composable
fun BloqueDeCodigo(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(rememberScrollState())
            .padding(12.dp),
    ) { contenido() }
}

@Composable
fun TextoDeCodigo(texto: String) {
    Text(texto, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
}
