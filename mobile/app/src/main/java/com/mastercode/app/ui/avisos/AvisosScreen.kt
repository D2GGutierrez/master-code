package com.mastercode.app.ui.avisos

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R

// HU52-T3 (CA-52.3, CA-52.4): el estudiante activa o desactiva los avisos de práctica y elige la hora. En Android 13 o más, activar
// los avisos pide el permiso de notificaciones (HU52-T1); si lo niega, quedan desactivados.
@Composable
fun AvisosScreen(viewModel: AvisosViewModel, alVolver: () -> Unit) {
    val ajustes by viewModel.ajustes.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    BackHandler(onBack = alVolver)

    // Antes de Android 13 las notificaciones no piden permiso.
    val pideSuPermiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val permitido = !pideSuPermiso ||
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        viewModel.activar(concedido)
    }
    // Los avisos vienen activados por defecto: la primera vez que se abre esta pantalla se pide el permiso que falta.
    LaunchedEffect(Unit) {
        if (ajustes.activados && !permitido) pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.avisos_titulo), style = MaterialTheme.typography.headlineMedium)
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.avisos_activar), style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = ajustes.activados,
                onCheckedChange = { quiere ->
                    if (quiere && !permitido) pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS) else viewModel.activar(quiere)
                },
            )
        }
        if (ajustes.activados) {
            val hora = stringResource(R.string.avisos_hora_formato, ajustes.hora, ajustes.minuto)
            Text(stringResource(R.string.avisos_hora_actual, hora))
            Button(
                onClick = {
                    TimePickerDialog(
                        contexto,
                        { _, h, m -> viewModel.elegirHora(h, m) },
                        ajustes.hora,
                        ajustes.minuto,
                        true,
                    ).show()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.avisos_cambiar_hora)) }
        }
        TextButton(onClick = alVolver, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.avisos_volver))
        }
    }
}
