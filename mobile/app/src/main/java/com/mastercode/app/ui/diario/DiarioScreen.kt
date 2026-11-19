package com.mastercode.app.ui.diario

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R
import com.mastercode.app.ui.componentes.BarraDeProgreso
import com.mastercode.app.ui.componentes.BotonPrincipal
import com.mastercode.app.ui.componentes.EtiquetaMC
import com.mastercode.app.ui.componentes.HeroMC
import com.mastercode.app.ui.componentes.LogoMC
import com.mastercode.app.ui.componentes.PantallaMC
import com.mastercode.app.ui.racha.EstadoDeRacha
import com.mastercode.app.ui.racha.MensajeDeRacha
import com.mastercode.app.ui.racha.RachaViewModel
import com.mastercode.app.ui.theme.FuegoDeRacha
import com.mastercode.app.ui.theme.MC
import com.mastercode.app.ui.theme.NaranjaDeBatalla
import com.mastercode.app.ui.theme.SobreHero
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.mastercode.app.ui.componentes.entrada
import com.mastercode.app.ui.componentes.TextoQueSeEscribe
import com.mastercode.app.ui.componentes.anilloGiratorio
import com.mastercode.app.ui.componentes.contadorAnimado
import com.mastercode.app.ui.componentes.resplandor
import com.mastercode.app.ui.componentes.VentanaMC

// La pantalla de inicio, como el «Diario» de Consoly: la racha, el botón grande para empezar a practicar, la meta del día y la puerta a
// las batallas. Solo muestra datos que la app tiene (la racha y si ya practicó hoy): no hay retos diarios ni puntos por ejercicio.
@Composable
fun DiarioScreen(
    viewModel: RachaViewModel,
    alIniciarPractica: () -> Unit,
    alAbrirBatallas: () -> Unit,
    alAbrirAvisos: () -> Unit,
    pie: @Composable () -> Unit,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val racha = estado as? EstadoDeRacha.ConRacha

    PantallaMC(
        cabecera = {
            HeroMC(transparente = true) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LogoMC(tamano = 40.dp)
                    Text("MASTER CODE", style = MaterialTheme.typography.labelMedium, color = SobreHero, modifier = Modifier.weight(1f))
                    BotonRedondo(Icons.Rounded.Notifications, stringResource(R.string.diario_avisos_descripcion), alAbrirAvisos)
                }
            }
        },
        pie = pie,
        conAurora = true,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.entrada(0), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EtiquetaMC(stringResource(R.string.diario_sobretitulo))
                // El título se escribe solo, como en una terminal, con el cursor azul parpadeando al final.
                TextoQueSeEscribe(
                    texto = stringResource(R.string.diario_titulo),
                    estilo = MaterialTheme.typography.headlineLarge,
                    retrasoMs = 150,
                    encabezado = true,
                    prefijo = "> ",
                )
            }
            if (racha != null) TarjetaDeRacha(racha, Modifier.entrada(70))
            BotonPrincipal(
                texto = stringResource(R.string.diario_iniciar),
                alTocar = alIniciarPractica,
                modifier = Modifier.fillMaxWidth().entrada(140),
                icono = Icons.Rounded.Bolt,
                destacado = true,
            )
            if (racha != null) TarjetaDeMeta(racha.practicoHoy, Modifier.entrada(210))
            TarjetaDeBatalla(alAbrirBatallas, Modifier.entrada(280))
        }
    }
}

// Un botón redondo de 44 dp con un ícono (los avisos arriba a la derecha).
@Composable
private fun BotonRedondo(icono: androidx.compose.ui.graphics.vector.ImageVector, descripcion: String, alTocar: () -> Unit) {
    val c = MC.colores
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(c.fondo, CircleShape)
            .border(1.dp, c.borde, CircleShape)
            .clickable(onClickLabel = descripcion, role = Role.Button, onClick = alTocar),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icono, contentDescription = descripcion, tint = c.texto, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun TarjetaDeRacha(racha: EstadoDeRacha.ConRacha, modifier: Modifier = Modifier) {
    val c = MC.colores
    // Con racha, la llama late despacio: crece y se achica un poco, sin parar.
    val pulso = rememberInfiniteTransition(label = "llama")
    val latido by pulso.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 900, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "latidoDeLlama",
    )
    val escalaDeLlama = if (racha.rachaActual > 0) latido else 1f
    VentanaMC(archivo = "racha.py", modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Con racha, la llama encendida dentro de un anillo de fuego que gira y un halo que respira; sin racha, la llama apagada
            // (en gris y más tenue) y un anillo gris quieto.
            val encendida = racha.rachaActual > 0
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .then(if (encendida) Modifier.resplandor(FuegoDeRacha, 38.dp, intensidad = 0.9f) else Modifier)
                    .background(c.lienzo, CircleShape)
                    .then(
                        if (encendida) {
                            Modifier.anilloGiratorio(listOf(FuegoDeRacha, NaranjaDeBatalla, FuegoDeRacha.copy(alpha = 0.08f), FuegoDeRacha), grosor = 2.5.dp)
                        } else {
                            Modifier.border(1.5.dp, c.borde, CircleShape)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ilu_racha),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            scaleX = escalaDeLlama
                            scaleY = escalaDeLlama
                        }
                        .alpha(if (encendida) 1f else 0.5f),
                    colorFilter = if (encendida) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (racha.rachaActual > 0) {
                    val titulo = if (racha.rachaActual == 1) R.string.racha_dias_uno else R.string.racha_dias_otros
                    // Los días suben desde cero al abrir el Diario.
                    val dias = contadorAnimado(racha.rachaActual, 900)
                    Text(stringResource(titulo, dias), style = MaterialTheme.typography.titleLarge, color = FuegoDeRacha)
                }
                val mensaje = when (racha.mensaje) {
                    MensajeDeRacha.Empezar -> R.string.racha_empezar
                    MensajeDeRacha.PracticarHoy -> R.string.racha_practicar_hoy
                    MensajeDeRacha.YaPractico -> R.string.racha_ya_practicaste
                }
                Text(stringResource(mensaje), style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
            }
        }
    }
}

// «META DE HOY 0/1»: la única meta que la app puede medir es haber practicado hoy.
@Composable
private fun TarjetaDeMeta(practicoHoy: Boolean, modifier: Modifier = Modifier) {
    val c = MC.colores
    val hecho = if (practicoHoy) 1 else 0
    VentanaMC(archivo = "meta_de_hoy.json", modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EtiquetaMC(stringResource(R.string.diario_meta_etiqueta), modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.diario_meta_texto, hecho, 1),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (practicoHoy) c.exito else c.texto,
                )
            }
            BarraDeProgreso(
                progreso = hecho.toFloat(),
                alto = 10.dp,
                colorDelRelleno = if (practicoHoy) c.exito else c.acento,
            )
            Text(
                stringResource(if (practicoHoy) R.string.diario_meta_cumplida else R.string.diario_meta_pendiente),
                style = MaterialTheme.typography.bodySmall,
                color = c.textoSuave,
            )
        }
    }
}

@Composable
private fun TarjetaDeBatalla(alTocar: () -> Unit, modifier: Modifier = Modifier) {
    val c = MC.colores
    VentanaMC(archivo = "batalla.sh", modifier = modifier.fillMaxWidth(), alTocar = alTocar) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = NaranjaDeBatalla, modifier = Modifier.size(32.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.diario_retar_titulo), style = MaterialTheme.typography.titleMedium, color = c.texto)
                Text(stringResource(R.string.diario_retar_texto), style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textoSuave, modifier = Modifier.size(24.dp))
        }
    }
}
