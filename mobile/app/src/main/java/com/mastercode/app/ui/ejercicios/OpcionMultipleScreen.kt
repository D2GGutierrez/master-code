package com.mastercode.app.ui.ejercicios

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R
import com.mastercode.app.ui.theme.MC

// HU46-T3 (CA-46.1, CA-46.2, CA-46.5): la pregunta y una alternativa por tarjeta, grandes para tocarlas sin errar. Se toca
// una para elegirla (queda marcada) y se envía con el botón de MarcoDeEjercicio. user-api no manda cuál es la correcta
// antes de responder (CA-46.4): después, MarcoDeEjercicio muestra la correcta si falló.
@Composable
fun OpcionMultipleScreen(viewModel: ControlDeEjercicio, alVencerSesion: () -> Unit) {
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
        if (partes.pregunta.isNotEmpty()) Text(partes.pregunta, style = MaterialTheme.typography.bodyLarge)
        if (partes.codigo.isNotEmpty()) {
            BloqueDeCodigo(modifier = Modifier.fillMaxWidth()) { TextoDeCodigo(partes.codigo) }
        }
        val marcada = stringResource(R.string.opcion_marcada)
        problema.opciones.forEachIndexed { indice, opcion ->
            OpcionDeRespuesta(
                letra = ('A' + indice).toString(),
                texto = opcion.texto,
                elegida = uiState.opcionElegida == opcion.id,
                habilitada = uiState.editable,
                marcada = marcada,
                alTocar = { viewModel.alElegirOpcion(opcion.id) },
            )
        }
    }
}

// La elegida va con borde de acento, fondo suave y la letra rellena: se distingue sin depender solo del color.
@Composable
private fun OpcionDeRespuesta(
    letra: String,
    texto: String,
    elegida: Boolean,
    habilitada: Boolean,
    marcada: String,
    alTocar: () -> Unit,
) {
    val c = MC.colores
    val forma = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ALTURA_MINIMA_OPCION_DP.dp)
            .clip(forma)
            .background(if (elegida) c.acentoSuave else c.fondo)
            .border(if (elegida) 2.dp else 1.dp, if (elegida) c.acento else c.borde, forma)
            .selectable(selected = elegida, enabled = habilitada, role = Role.RadioButton, onClick = alTocar)
            .semantics { if (elegida) stateDescription = marcada }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(if (elegida) c.acento else c.fondoSuave, CircleShape)
                .border(1.dp, if (elegida) c.acento else c.bordeFuerte, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(letra, style = MaterialTheme.typography.labelLarge, color = if (elegida) c.sobreAcento else c.textoSuave)
        }
        Text(
            texto,
            style = MaterialTheme.typography.bodyLarge,
            color = if (habilitada || elegida) c.texto else c.textoSuave,
            modifier = Modifier.weight(1f),
        )
    }
}
