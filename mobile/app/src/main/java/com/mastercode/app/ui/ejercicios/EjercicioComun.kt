package com.mastercode.app.ui.ejercicios

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mastercode.app.R
import com.mastercode.app.data.problemas.ProblemaDelEjercicio
import com.mastercode.app.ui.componentes.BotonPrincipal
import com.mastercode.app.ui.componentes.BotonSecundario
import com.mastercode.app.ui.componentes.ChipMC
import com.mastercode.app.ui.componentes.EtiquetaMC
import com.mastercode.app.ui.componentes.IntentosMC
import com.mastercode.app.ui.componentes.MensajeDeError
import com.mastercode.app.ui.componentes.TarjetaMC
import com.mastercode.app.ui.componentes.TemporizadorMC
import com.mastercode.app.ui.componentes.formatoDeTiempo
import com.mastercode.app.ui.theme.EstiloDeCodigo
import com.mastercode.app.ui.theme.MC
import kotlinx.coroutines.delay
import androidx.compose.runtime.key
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.mastercode.app.ui.componentes.ConfetiMC
import com.mastercode.app.ui.componentes.aparecerConRebote
import com.mastercode.app.ui.componentes.entrada

// Lo que las pantallas de ejercicio comparten: la fila «PRÁCTICA» con el tiempo, los chips de intentos y de tipo, el título, la
// carga, los errores y el envío de la respuesta (botón, resultado e intentos que quedan). Solo el contenido «Listo» cambia. No toca
// los bordes de la pantalla: de eso se ocupa quien la contiene (el Dojo o la partida).
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
            // key: cada ejercicio empieza de cero (tiempo, desplazamiento y animación de entrada), aunque la pantalla sea la misma.
            is ContenidoEjercicio.Listo -> key(contenido.problema.id) {
                val problemaId = contenido.problema.id
                val envio = uiState.envio
                val resuelto = envio is EstadoDeEnvio.Resultado
                // El tiempo corre hasta que el ejercicio queda respondido; cada ejercicio empieza en cero.
                var segundos by rememberSaveable(problemaId) { mutableIntStateOf(0) }
                LaunchedEffect(problemaId, resuelto) {
                    if (!resuelto) {
                        while (true) {
                            delay(1000)
                            segundos += 1
                        }
                    }
                }
                val vibracion = LocalHapticFeedback.current
                // Al corregirse la respuesta, una vibración corta le avisa aunque no esté mirando la pantalla.
                LaunchedEffect(resuelto) {
                    if (resuelto) vibracion.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                val desplazamiento = rememberScrollState()
                // Al responder, el veredicto aparece arriba: se sube para verlo sin buscarlo.
                LaunchedEffect(resuelto) {
                    if (resuelto) desplazamiento.animateScrollTo(0)
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxSize().entrada(0).verticalScroll(desplazamiento).padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (envio is EstadoDeEnvio.Resultado) {
                            VeredictoDelEjercicio(envio.acerto, envio.respuestaCorrecta, segundos)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            EtiquetaMC(stringResource(R.string.ejercicio_practica))
                            TemporizadorMC(segundos)
                        }
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // En el Dojo cada ejercicio se responde una sola vez (el servidor solo acepta una respuesta por ejercicio en
                            // la sesión): mostrar «intentos restantes» ahí prometería reintentos que no existen.
                            if (!uiState.enSesion) uiState.intentosRestantes?.let { IntentosMC(it) }
                            etiquetaDelTipo(contenido.problema.tipoEjercicio)?.let { ChipMC(it) }
                        }
                        Text(
                            contenido.problema.titulo,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics { heading() },
                        )
                        contenidoListo(contenido.problema)
                        PanelDeEnvio(uiState, alEnviar, alIntentarDeNuevo)
                    }
                    // Si acertó, cae confeti un momento sobre el ejercicio.
                    if (envio is EstadoDeEnvio.Resultado && envio.acerto) ConfetiMC(modifier = Modifier.fillMaxSize(), cantidad = 40, duracionMs = 2200)
                }
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

// HU45-T3, HU48-T2, HU48-T3: el botón para enviar y, debajo, lo que pasó con la respuesta: que quedó guardada por falta de
// conexión o por qué no se pudo enviar; siempre con los intentos que quedan. El veredicto (acertó o no) va arriba de todo.
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
        is EstadoDeEnvio.Resultado -> Unit
    }
    if (!uiState.enSesion) {
        uiState.intentosRestantes?.let { restantes ->
            Text(textoDeIntentos(restantes), style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
        }
    }
    if (uiState.puedeIntentarDeNuevo) {
        BotonSecundario(
            texto = stringResource(R.string.ejercicio_intentar_de_nuevo),
            alTocar = alIntentarDeNuevo,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// El veredicto como en la pantalla «Resuelto» de Consoly: título grande, una frase en verde o rojo, el tiempo con un cuadrito de
// color y, si falló, una tarjeta con la respuesta correcta.
@Composable
private fun VeredictoDelEjercicio(acerto: Boolean, respuestaCorrecta: String?, segundos: Int) {
    val c = MC.colores
    val color = if (acerto) c.exito else c.error
    Column(
        modifier = Modifier.fillMaxWidth().aparecerConRebote().semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(if (acerto) R.string.ejercicio_resuelto else R.string.ejercicio_fallo),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(if (acerto) R.string.ejercicio_bien_hecho else R.string.ejercicio_sigue_practicando),
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            textAlign = TextAlign.Center,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                formatoDeTiempo(segundos),
                style = EstiloDeCodigo.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
                color = c.texto,
            )
            Box(modifier = Modifier.size(24.dp).background(color, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                Icon(
                    if (acerto) Icons.Rounded.Check else Icons.Rounded.Close,
                    contentDescription = null,
                    tint = c.lienzo,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        if (!acerto && respuestaCorrecta != null) {
            TarjetaMC(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EtiquetaMC(stringResource(R.string.ejercicio_respuesta_etiqueta), color = c.acento)
                    Text(respuestaCorrecta, style = EstiloDeCodigo.copy(fontSize = 16.sp, lineHeight = 24.sp), color = c.texto)
                }
            }
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
