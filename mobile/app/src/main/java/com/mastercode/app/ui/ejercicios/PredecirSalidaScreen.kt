package com.mastercode.app.ui.ejercicios

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R

// HU45-T2 (CA-45.1, CA-45.2): el fragmento de código y un campo para escribir lo que imprimiría. Todavía no envía la
// respuesta (HU45-T3) ni se llega a ella desde otra pantalla (el Dojo, HU53-T3).
@Composable
fun PredecirSalidaScreen(viewModel: EjercicioViewModel, alVencerSesion: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MarcoDeEjercicio(uiState, alReintentar = viewModel::cargar, alVencerSesion = alVencerSesion) { enunciado ->
        val partes = separarCodigo(enunciado)
        if (partes.pregunta.isNotEmpty()) Text(partes.pregunta)
        if (partes.codigo.isNotEmpty()) {
            BloqueDeCodigo(modifier = Modifier.fillMaxWidth()) { TextoDeCodigo(partes.codigo) }
        }
        // La salida puede tener varias líneas: el campo crece con lo que se escribe.
        OutlinedTextField(
            value = uiState.respuesta,
            onValueChange = viewModel::alCambiarRespuesta,
            label = { Text(stringResource(R.string.predecir_campo)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
