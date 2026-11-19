package com.mastercode.app.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.mastercode.app.BuildConfig
import com.mastercode.app.R
import com.mastercode.app.ui.componentes.BotonPrincipal
import com.mastercode.app.ui.componentes.BotonSecundario
import com.mastercode.app.ui.componentes.CampoDeTextoMC
import com.mastercode.app.ui.componentes.HeroMC
import com.mastercode.app.ui.componentes.LogoMC
import com.mastercode.app.ui.componentes.MensajeDeError
import com.mastercode.app.ui.componentes.PantallaMC
import com.mastercode.app.ui.componentes.TarjetaMC
import com.mastercode.app.ui.theme.MC
import com.mastercode.app.ui.theme.SobreHero
import com.mastercode.app.ui.theme.SobreHeroSuave

@Composable
fun LoginScreen(viewModel: LoginViewModel, alIniciarSesion: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var verContrasena by rememberSaveable { mutableStateOf(false) }
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()

    LaunchedEffect(uiState.sesionIniciada) {
        if (uiState.sesionIniciada) alIniciarSesion()
    }

    PantallaMC(
        cabecera = {
            HeroMC {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LogoMC(tamano = 68.dp)
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = SobreHero)
                    Text(
                        stringResource(R.string.marca_lema),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SobreHeroSuave,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
    ) {
        // Como el inicio de sesión de GitHub: una tarjeta con borde fino sobre el lienzo gris.
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            TarjetaMC(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        stringResource(R.string.login_titulo),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(stringResource(R.string.login_bienvenida), style = MaterialTheme.typography.bodyMedium, color = MC.colores.textoSuave)
                    CampoDeTextoMC(
                        valor = uiState.email,
                        alCambiar = viewModel::alCambiarEmail,
                        etiqueta = stringResource(R.string.login_correo),
                        unaLinea = true,
                        habilitado = !uiState.cargando,
                        iconoInicial = { Icon(Icons.Rounded.Email, contentDescription = null) },
                        opcionesDeTeclado = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val textoDelOjo = stringResource(if (verContrasena) R.string.login_ocultar_contrasena else R.string.login_mostrar_contrasena)
                    CampoDeTextoMC(
                        valor = uiState.password,
                        alCambiar = viewModel::alCambiarPassword,
                        etiqueta = stringResource(R.string.login_contrasena),
                        unaLinea = true,
                        habilitado = !uiState.cargando,
                        iconoInicial = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        iconoFinal = {
                            IconButton(onClick = { verContrasena = !verContrasena }) {
                                Icon(if (verContrasena) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = textoDelOjo)
                            }
                        },
                        transformacionVisual = if (verContrasena) VisualTransformation.None else PasswordVisualTransformation(),
                        opcionesDeTeclado = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    uiState.error?.let {
                        MensajeDeError(stringResource(it), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    BotonPrincipal(
                        texto = stringResource(R.string.login_boton),
                        alTocar = viewModel::alIniciarSesion,
                        habilitado = uiState.puedeEnviar,
                        cargando = uiState.cargando,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                    // Sin ID de cliente (google.client.id en local.properties) el botón no se muestra: la contraseña sigue sirviendo.
                    if (BuildConfig.GOOGLE_CLIENT_ID.isNotBlank()) {
                        Text(
                            stringResource(R.string.login_o),
                            style = MaterialTheme.typography.bodySmall,
                            color = MC.colores.textoSuave,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        BotonSecundario(
                            texto = stringResource(R.string.login_google),
                            alTocar = {
                                alcance.launch {
                                    when (val resultado = pedirTokenDeGoogle(contexto, BuildConfig.GOOGLE_CLIENT_ID)) {
                                        is ResultadoGoogle.Token -> viewModel.alRecibirTokenDeGoogle(resultado.idToken)
                                        ResultadoGoogle.NoDisponible -> viewModel.alFallarGoogle(R.string.login_error_google_no_disponible)
                                        ResultadoGoogle.Cancelado -> Unit
                                    }
                                }
                            },
                            habilitado = !uiState.cargando,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
