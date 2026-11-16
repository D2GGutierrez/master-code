package com.mastercode.app.ui.ejercicios

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mastercode.app.R

// Lo que las dos pantallas de ejercicio comparten: el título, la carga y los errores. Solo el contenido «Listo» cambia.
@Composable
fun MarcoDeEjercicio(
    uiState: EjercicioUiState,
    alReintentar: () -> Unit,
    alVencerSesion: () -> Unit,
    contenidoListo: @Composable (enunciado: String) -> Unit,
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
                contenidoListo(contenido.problema.enunciado)
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
