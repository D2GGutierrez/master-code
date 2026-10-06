package com.mastercode.app.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R

@Composable
fun LoginScreen(viewModel: LoginViewModel, alIniciarSesion: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.sesionIniciada) {
        if (uiState.sesionIniciada) alIniciarSesion()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.login_titulo), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::alCambiarEmail,
            label = { Text(stringResource(R.string.login_correo)) },
            singleLine = true,
            enabled = !uiState.cargando,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::alCambiarPassword,
            label = { Text(stringResource(R.string.login_contrasena)) },
            singleLine = true,
            enabled = !uiState.cargando,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        uiState.error?.let {
            Text(stringResource(it), color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = viewModel::alIniciarSesion,
            enabled = uiState.puedeEnviar,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.cargando) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.login_boton))
            }
        }
    }
}
