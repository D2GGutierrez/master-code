package com.mastercode.app.ui.dojo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mastercode.app.R
import com.mastercode.app.data.dojo.ResumenDeSesion
import com.mastercode.app.ui.cinturon.nombreDeCinturon

// HU56-T2 (CA-56.1, CA-56.2, CA-56.4, CA-56.5): el resumen al terminar la sesión del Dojo: aciertos de respondidos, puntos
// sumados, el cinturón y los temas a repasar. Cubre la sesión sin errores (sin temas) y la sesión sin aciertos.
@Composable
fun ResumenDeSesionScreen(resumen: ResumenDeSesion, alVolver: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.dojo_resumen_titulo), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.dojo_resumen_aciertos, resumen.aciertos, resumen.respondidos),
            style = MaterialTheme.typography.titleLarge,
        )
        val mensaje = when (tipoDeResumen(resumen)) {
            TipoDeResumen.SinRespuestas -> R.string.resumen_sin_respuestas
            TipoDeResumen.Perfecto -> R.string.resumen_perfecto
            TipoDeResumen.SinAciertos -> R.string.resumen_sin_aciertos
            TipoDeResumen.Normal -> R.string.resumen_normal
        }
        Text(stringResource(mensaje))
        Text(stringResource(R.string.dojo_resumen_puntos, resumen.puntosSumados))
        if (resumen.cinturonNivel != null && resumen.cinturonPuntos != null) {
            val nombre = nombreDeCinturon(resumen.cinturonNivel)?.let { stringResource(it) } ?: resumen.cinturonNivel.toString()
            Text(stringResource(R.string.resumen_cinturon, nombre, resumen.cinturonPuntos))
        }
        // CA-56.2: los temas donde más falló, del que más al que menos; con una sesión perfecta no hay ninguno.
        if (resumen.temasMasFallados.isNotEmpty()) {
            Text(stringResource(R.string.resumen_temas_titulo), style = MaterialTheme.typography.titleMedium)
            for (tema in resumen.temasMasFallados) {
                val texto = if (tema.fallos == 1) {
                    stringResource(R.string.resumen_tema_fallos_uno, tema.tema)
                } else {
                    stringResource(R.string.resumen_tema_fallos_otros, tema.tema, tema.fallos)
                }
                Text(texto)
            }
        }
        Button(onClick = alVolver, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.dojo_volver))
        }
    }
}
