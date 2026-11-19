package com.mastercode.app.ui.avisos

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R
import com.mastercode.app.ui.componentes.BotonSecundario
import com.mastercode.app.ui.componentes.CabeceraMC
import com.mastercode.app.ui.componentes.PantallaMC
import com.mastercode.app.ui.componentes.TarjetaMC
import com.mastercode.app.ui.theme.EstiloDeCodigo
import com.mastercode.app.ui.theme.MC

// HU52-T3 (CA-52.3, CA-52.4): el estudiante activa o desactiva los avisos de práctica y elige la hora. En Android 13 o más, activar
// los avisos pide el permiso de notificaciones (HU52-T1); si lo niega, quedan desactivados.
@Composable
fun AvisosScreen(viewModel: AvisosViewModel, alVolver: () -> Unit) {
    val ajustes by viewModel.ajustes.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val c = MC.colores

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

    PantallaMC(
        cabecera = {
            CabeceraMC(
                titulo = stringResource(R.string.avisos_titulo),
                sobretitulo = stringResource(R.string.avisos_sobretitulo),
                alVolver = alVolver,
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.avisos_explicacion), style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
            TarjetaMC(modifier = Modifier.fillMaxWidth()) {
                // Toda la fila es el interruptor: se toca en cualquier parte y los lectores de pantalla la leen como un interruptor.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = ajustes.activados,
                            role = Role.Switch,
                            onValueChange = { quiere ->
                                if (quiere && !permitido) pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS) else viewModel.activar(quiere)
                            },
                        )
                        .heightIn(min = 72.dp)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(modifier = Modifier.size(44.dp).background(c.acentoSuave, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Notifications, contentDescription = null, tint = c.acento, modifier = Modifier.size(24.dp))
                    }
                    Text(stringResource(R.string.avisos_activar), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Switch(
                        checked = ajustes.activados,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = c.sobreAcento,
                            checkedTrackColor = c.acento,
                            uncheckedThumbColor = c.textoSuave,
                            uncheckedTrackColor = c.fondoSuave,
                            uncheckedBorderColor = c.bordeFuerte,
                        ),
                    )
                }
            }
            if (ajustes.activados) {
                val hora = stringResource(R.string.avisos_hora_formato, ajustes.hora, ajustes.minuto)
                TarjetaMC(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = c.textoSuave, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.avisos_hora_titulo).uppercase(), style = MaterialTheme.typography.labelSmall, color = c.textoSuave)
                        }
                        // La hora grande y en letra de código, como un reloj.
                        Text(hora, style = EstiloDeCodigo.copy(fontSize = 48.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold), color = c.texto)
                        Text(stringResource(R.string.avisos_hora_actual, hora), style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
                        BotonSecundario(
                            texto = stringResource(R.string.avisos_cambiar_hora),
                            alTocar = {
                                TimePickerDialog(
                                    contexto,
                                    { _, h, m -> viewModel.elegirHora(h, m) },
                                    ajustes.hora,
                                    ajustes.minuto,
                                    true,
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
