package com.mastercode.app.ui.ejercicios

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R
import com.mastercode.app.ui.componentes.CampoDeTextoMC
import com.mastercode.app.ui.theme.EstiloDeCodigo

// HU45-T2 (CA-45.1, CA-45.2): el fragmento de código y un campo para escribir lo que imprimiría. El envío, el resultado
// y los intentos los pone MarcoDeEjercicio (HU45-T3, HU48-T2); se llega a ella desde el Dojo (HU53-T3).
@Composable
fun PredecirSalidaScreen(viewModel: ControlDeEjercicio, alVencerSesion: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MarcoDeEjercicio(
        uiState,
        alReintentar = viewModel::cargar,
        alVencerSesion = alVencerSesion,
        alEnviar = viewModel::enviar,
        alIntentarDeNuevo = viewModel::intentarDeNuevo,
    ) { problema ->
        val partes = separarCodigo(problema.enunciado)
        if (partes.pregunta.isNotEmpty()) Text(partes.pregunta, style = MaterialTheme.typography.bodyLarge)
        if (partes.codigo.isNotEmpty()) {
            BloqueDeCodigo(modifier = Modifier.fillMaxWidth()) { TextoDeCodigo(partes.codigo) }
        }
        // La salida puede tener varias líneas: el campo crece con lo que se escribe.
        CampoDeTextoMC(
            valor = uiState.respuesta,
            alCambiar = viewModel::alCambiarRespuesta,
            etiqueta = stringResource(R.string.predecir_campo),
            lineasMinimas = 2,
            habilitado = uiState.editable,
            estiloDeTexto = EstiloDeCodigo,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
