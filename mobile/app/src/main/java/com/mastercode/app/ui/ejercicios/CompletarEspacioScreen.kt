package com.mastercode.app.ui.ejercicios

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R
import com.mastercode.app.ui.componentes.CampoDeTextoMC
import com.mastercode.app.ui.theme.EstiloDeCodigo

// HU47-T3 (CA-47.1, CA-47.2): el fragmento de código con el campo de texto en el lugar del hueco. El envío, el resultado
// y los intentos los pone MarcoDeEjercicio (HU48-T2); se llega a ella desde el Dojo (HU53-T3).
@Composable
fun CompletarEspacioScreen(viewModel: ControlDeEjercicio, alVencerSesion: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MarcoDeEjercicio(
        uiState,
        alReintentar = viewModel::cargar,
        alVencerSesion = alVencerSesion,
        alEnviar = viewModel::enviar,
        alIntentarDeNuevo = viewModel::intentarDeNuevo,
    ) { problema ->
        val enunciado = problema.enunciado
        val partes = separarHueco(enunciado)
        if (partes == null) {
            // Un enunciado sin hueco no debería existir (admin-api no lo aprueba, D17): se muestra y se deja el campo aparte.
            Text(enunciado, style = MaterialTheme.typography.bodyLarge)
            CampoDelHueco(uiState.respuesta, uiState.editable, viewModel::alCambiarRespuesta, Modifier.fillMaxWidth(), sobreCodigo = false)
        } else {
            CodigoConHueco(partes) { CampoDelHueco(uiState.respuesta, uiState.editable, viewModel::alCambiarRespuesta, Modifier.width(150.dp), sobreCodigo = true) }
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
private fun CampoDelHueco(valor: String, editable: Boolean, alCambiar: (String) -> Unit, modifier: Modifier, sobreCodigo: Boolean) {
    // El hueco solo tiene «___» de marca: el lector de pantalla necesita decir qué es el campo.
    val descripcion = stringResource(R.string.completar_campo)
    CampoDeTextoMC(
        valor = valor,
        alCambiar = alCambiar,
        habilitado = editable,
        placeholder = "___",
        unaLinea = true,
        estiloDeTexto = EstiloDeCodigo,
        sobreCodigo = sobreCodigo,
        modifier = modifier.semantics { contentDescription = descripcion },
    )
}
