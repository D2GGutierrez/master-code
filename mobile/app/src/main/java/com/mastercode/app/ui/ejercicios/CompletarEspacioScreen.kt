package com.mastercode.app.ui.ejercicios

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R

// HU47-T3 (CA-47.1, CA-47.2): el fragmento de código con el campo de texto en el lugar del hueco. Todavía no envía la
// respuesta (HU48-T2) ni se llega a ella desde otra pantalla (el Dojo, HU53-T3).
@Composable
fun CompletarEspacioScreen(viewModel: EjercicioViewModel, alVencerSesion: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MarcoDeEjercicio(uiState, alReintentar = viewModel::cargar, alVencerSesion = alVencerSesion) { enunciado ->
        val partes = separarHueco(enunciado)
        if (partes == null) {
            // Un enunciado sin hueco no debería existir (admin-api no lo aprueba, D17): se muestra y se deja el campo aparte.
            Text(enunciado)
            CampoDelHueco(uiState.respuesta, viewModel::alCambiarRespuesta, Modifier.fillMaxWidth())
        } else {
            CodigoConHueco(partes) { CampoDelHueco(uiState.respuesta, viewModel::alCambiarRespuesta, Modifier.width(150.dp)) }
        }
    }
}

// Las líneas antes y después del hueco van como texto; la línea del hueco es una fila: lo que la precede, el campo y
// lo que la sigue.
@Composable
private fun CodigoConHueco(partes: EnunciadoConHueco, campo: @Composable () -> Unit) {
    val antes = partes.antes.split("\n")
    val despues = partes.despues.split("\n")
    BloqueDeCodigo(modifier = Modifier.fillMaxWidth()) {
        Column {
            if (antes.size > 1) TextoDeCodigo(antes.dropLast(1).joinToString("\n"))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (antes.last().isNotEmpty()) TextoDeCodigo(antes.last())
                campo()
                if (despues.first().isNotEmpty()) TextoDeCodigo(despues.first())
            }
            if (despues.size > 1) TextoDeCodigo(despues.drop(1).joinToString("\n"))
        }
    }
}

@Composable
private fun CampoDelHueco(valor: String, alCambiar: (String) -> Unit, modifier: Modifier) {
    // El hueco solo tiene «___» de marca: el lector de pantalla necesita decir qué es el campo.
    val descripcion = stringResource(R.string.completar_campo)
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        placeholder = { Text("___", fontFamily = FontFamily.Monospace) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        modifier = modifier.semantics { contentDescription = descripcion },
    )
}
