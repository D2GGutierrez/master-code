package com.mastercode.app.ui.ejercicios

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R

// HU46-T3 (CA-46.1, CA-46.2, CA-46.5): la pregunta y una alternativa por botón, grandes para tocarlas sin errar. Se toca
// una para elegirla (queda marcada) y se envía con el botón de MarcoDeEjercicio. user-api no manda cuál es la correcta
// antes de responder (CA-46.4): después, MarcoDeEjercicio muestra la correcta si falló.
@Composable
fun OpcionMultipleScreen(viewModel: EjercicioViewModel, alVencerSesion: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MarcoDeEjercicio(
        uiState,
        alReintentar = viewModel::cargar,
        alVencerSesion = alVencerSesion,
        alEnviar = viewModel::enviar,
        alIntentarDeNuevo = viewModel::intentarDeNuevo,
    ) { problema ->
        // La pregunta puede traer un fragmento de código: se muestra igual que al predecir la salida.
        val partes = separarCodigo(problema.enunciado)
        if (partes.pregunta.isNotEmpty()) Text(partes.pregunta)
        if (partes.codigo.isNotEmpty()) {
            BloqueDeCodigo(modifier = Modifier.fillMaxWidth()) { TextoDeCodigo(partes.codigo) }
        }
        val marcada = stringResource(R.string.opcion_marcada)
        for (opcion in problema.opciones) {
            val elegida = uiState.opcionElegida == opcion.id
            val modificador = Modifier
                .fillMaxWidth()
                .heightIn(min = ALTURA_MINIMA_OPCION_DP.dp)
                .semantics {
                    selected = elegida
                    if (elegida) stateDescription = marcada
                }
            // La elegida va rellena y las demás con borde: se distingue sin depender solo del color.
            if (elegida) {
                Button(onClick = { viewModel.alElegirOpcion(opcion.id) }, enabled = uiState.editable, modifier = modificador) {
                    Text(opcion.texto, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                }
            } else {
                OutlinedButton(onClick = { viewModel.alElegirOpcion(opcion.id) }, enabled = uiState.editable, modifier = modificador) {
                    Text(opcion.texto, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
