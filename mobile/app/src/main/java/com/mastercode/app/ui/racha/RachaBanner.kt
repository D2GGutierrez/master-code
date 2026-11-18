package com.mastercode.app.ui.racha

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R

// HU49-T3 (CA-49.3): la racha actual arriba de la pantalla principal. Mientras carga o si no se pudo saber, no ocupa lugar.
@Composable
fun RachaBanner(viewModel: RachaViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val racha = estado as? EstadoDeRacha.ConRacha ?: return

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
            if (racha.rachaActual > 0) {
                val titulo = if (racha.rachaActual == 1) R.string.racha_dias_uno else R.string.racha_dias_otros
                Text(stringResource(titulo, racha.rachaActual), style = MaterialTheme.typography.titleMedium)
            }
            val mensaje = when (racha.mensaje) {
                MensajeDeRacha.Empezar -> R.string.racha_empezar
                MensajeDeRacha.PracticarHoy -> R.string.racha_practicar_hoy
                MensajeDeRacha.YaPractico -> R.string.racha_ya_practicaste
            }
            Text(stringResource(mensaje))
        }
    }
}
