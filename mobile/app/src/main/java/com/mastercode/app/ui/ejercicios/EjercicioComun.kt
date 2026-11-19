package com.mastercode.app.ui.ejercicios

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mastercode.app.R
import com.mastercode.app.data.problemas.ProblemaDelEjercicio
import com.mastercode.app.ui.componentes.BotonPrincipal
import com.mastercode.app.ui.componentes.BotonSecundario
import com.mastercode.app.ui.componentes.InsigniaMC
import com.mastercode.app.ui.componentes.MensajeDeError
import com.mastercode.app.ui.componentes.TarjetaMC
import com.mastercode.app.ui.componentes.TonoDeInsignia
import com.mastercode.app.ui.theme.Barra
import com.mastercode.app.ui.theme.BordeSobreHero
import com.mastercode.app.ui.theme.EstiloDeCodigo
import com.mastercode.app.ui.theme.MC
import com.mastercode.app.ui.theme.SobreHero

// Lo que las pantallas de ejercicio comparten: el tipo y el título, la carga, los errores y el envío de la respuesta (botón,
// resultado e intentos que quedan). Solo el contenido «Listo» cambia. No toca los bordes de la pantalla: de eso se ocupa quien
// la contiene (el Dojo o la partida).
@Composable
fun MarcoDeEjercicio(
    uiState: EjercicioUiState,
    alReintentar: () -> Unit,
    alVencerSesion: () -> Unit,
    alEnviar: () -> Unit,
    alIntentarDeNuevo: () -> Unit,
    contenidoListo: @Composable (problema: ProblemaDelEjercicio) -> Unit,
) {
    LaunchedEffect(uiState.contenido) {
        if (uiState.contenido == ContenidoEjercicio.SesionVencida) alVencerSesion()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when (val contenido = uiState.contenido) {
            ContenidoEjercicio.Cargando, ContenidoEjercicio.SesionVencida -> Centrado {
                val descripcion = stringResource(R.string.ejercicio_cargando)
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = descripcion })
            }
            is ContenidoEjercicio.Error -> Centrado {
                MensajeDeError(stringResource(contenido.mensaje))
                BotonSecundario(
                    texto = stringResource(R.string.ejercicio_reintentar),
                    alTocar = alReintentar,
                    icono = Icons.Rounded.Refresh,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            is ContenidoEjercicio.Listo -> Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                etiquetaDelTipo(contenido.problema.tipoEjercicio)?.let { InsigniaMC(it, tono = TonoDeInsignia.Acento) }
                Text(
                    contenido.problema.titulo,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                contenidoListo(contenido.problema)
                PanelDeEnvio(uiState, alEnviar, alIntentarDeNuevo)
            }
        }
    }
}

@Composable
private fun etiquetaDelTipo(tipo: String): String? = when (tipo) {
    TIPO_PREDICCION_SALIDA -> stringResource(R.string.ejercicio_tipo_predecir)
    TIPO_COMPLETAR_ESPACIO -> stringResource(R.string.ejercicio_tipo_completar)
    TIPO_OPCION_MULTIPLE -> stringResource(R.string.ejercicio_tipo_opcion)
    else -> null
}

// HU45-T3, HU48-T2, HU48-T3: el botón para enviar y, debajo, lo que pasó con la respuesta: si acertó o cuál era la
// correcta, que quedó guardada por falta de conexión, o por qué no se pudo enviar; siempre con los intentos que quedan.
@Composable
private fun PanelDeEnvio(uiState: EjercicioUiState, alEnviar: () -> Unit, alIntentarDeNuevo: () -> Unit) {
    val c = MC.colores
    when (val envio = uiState.envio) {
        EstadoDeEnvio.SinEnviar, is EstadoDeEnvio.Rechazado -> {
            if (envio is EstadoDeEnvio.Rechazado) {
                MensajeDeError(stringResource(envio.mensaje))
            }
            BotonPrincipal(
                texto = stringResource(R.string.ejercicio_enviar),
                alTocar = alEnviar,
                habilitado = uiState.puedeEnviar,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        EstadoDeEnvio.Enviando -> Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            val descripcion = stringResource(R.string.ejercicio_enviando)
            CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = descripcion })
        }
        EstadoDeEnvio.Pendiente -> TarjetaMC(modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.WifiOff, contentDescription = null, tint = c.textoSuave, modifier = Modifier.size(24.dp))
                Text(stringResource(R.string.ejercicio_pendiente), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
        }
        is EstadoDeEnvio.Resultado -> ResultadoDeEnvio(envio.acerto, envio.respuestaCorrecta)
    }
    uiState.intentosRestantes?.let { restantes ->
        Text(textoDeIntentos(restantes), style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
    }
    if (uiState.puedeIntentarDeNuevo) {
        BotonSecundario(
            texto = stringResource(R.string.ejercicio_intentar_de_nuevo),
            alTocar = alIntentarDeNuevo,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// El veredicto: borde, ícono y título en verde o rojo (solo como texto y trazo, nunca como fondo) y, si falló, la respuesta correcta.
@Composable
private fun ResultadoDeEnvio(acerto: Boolean, respuestaCorrecta: String?) {
    val c = MC.colores
    val color = if (acerto) c.exito else c.error
    val forma = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.fondo, forma)
            .border(1.5.dp, color.copy(alpha = 0.6f), forma)
            .padding(16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(if (acerto) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Text(
                stringResource(if (acerto) R.string.ejercicio_acerto else R.string.ejercicio_fallo),
                style = MaterialTheme.typography.titleLarge,
                color = color,
            )
        }
        if (!acerto && respuestaCorrecta != null) {
            Text(stringResource(R.string.ejercicio_respuesta_correcta, respuestaCorrecta), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun textoDeIntentos(restantes: Int): String = when {
    restantes <= 0 -> stringResource(R.string.ejercicio_intentos_ninguno)
    restantes == 1 -> stringResource(R.string.ejercicio_intentos_uno)
    else -> stringResource(R.string.ejercicio_intentos_varios, restantes)
}

@Composable
private fun Centrado(contenido: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { contenido() }
    }
}

// Un fragmento de código como en un editor: fondo oscuro en los dos temas, letra de ancho fijo y, si una línea no cabe, se
// desliza de lado en vez de cortarse.
@Composable
fun BloqueDeCodigo(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    val forma = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(forma)
            .background(Barra)
            .border(1.dp, BordeSobreHero, forma)
            .horizontalScroll(rememberScrollState())
            .padding(16.dp),
    ) { contenido() }
}

@Composable
fun TextoDeCodigo(texto: String) {
    Text(texto, style = EstiloDeCodigo, color = SobreHero)
}
