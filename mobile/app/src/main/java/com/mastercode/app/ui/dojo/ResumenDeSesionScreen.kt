package com.mastercode.app.ui.dojo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mastercode.app.R
import com.mastercode.app.data.dojo.ResumenDeSesion
import com.mastercode.app.ui.cinturon.nombreDeCinturon
import com.mastercode.app.ui.componentes.AnilloDeProgreso
import com.mastercode.app.ui.componentes.BotonPrincipal
import com.mastercode.app.ui.componentes.HeroMC
import com.mastercode.app.ui.componentes.InsigniaMC
import com.mastercode.app.ui.componentes.TarjetaMC
import com.mastercode.app.ui.componentes.TonoDeInsignia
import com.mastercode.app.ui.theme.BordeSobreHero
import com.mastercode.app.ui.theme.EstiloDeCodigo
import com.mastercode.app.ui.theme.FuegoDeRacha
import com.mastercode.app.ui.theme.HeroBrillo
import com.mastercode.app.ui.theme.MC
import com.mastercode.app.ui.theme.SobreHero
import com.mastercode.app.ui.theme.SobreHeroSuave
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding

// HU56-T2 (CA-56.1, CA-56.2, CA-56.4, CA-56.5): el resumen al terminar la sesión del Dojo: aciertos de respondidos, puntos
// sumados, el cinturón y los temas a repasar. Cubre la sesión sin errores (sin temas) y la sesión sin aciertos.
@Composable
fun ResumenDeSesionScreen(resumen: ResumenDeSesion, alVolver: () -> Unit) {
    val c = MC.colores
    val aciertosEnTexto = stringResource(R.string.dojo_resumen_aciertos, resumen.aciertos, resumen.respondidos)
    val mensaje = when (tipoDeResumen(resumen)) {
        TipoDeResumen.SinRespuestas -> R.string.resumen_sin_respuestas
        TipoDeResumen.Perfecto -> R.string.resumen_perfecto
        TipoDeResumen.SinAciertos -> R.string.resumen_sin_aciertos
        TipoDeResumen.Normal -> R.string.resumen_normal
    }
    Column(modifier = Modifier.fillMaxSize().background(c.lienzo).verticalScroll(rememberScrollState())) {
        HeroMC {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    stringResource(R.string.dojo_resumen_titulo),
                    style = MaterialTheme.typography.headlineMedium,
                    color = SobreHero,
                    modifier = Modifier.semantics { heading() },
                )
                // El anillo: aciertos de respondidos. Para el lector de pantalla se dice con la frase completa.
                val fraccion = if (resumen.respondidos > 0) resumen.aciertos.toFloat() / resumen.respondidos else 0f
                AnilloDeProgreso(
                    progreso = fraccion,
                    tamano = 148.dp,
                    grosor = 12.dp,
                    colorDeLaPista = BordeSobreHero,
                    colorDelArco = HeroBrillo,
                    modifier = Modifier.semantics { contentDescription = aciertosEnTexto },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.resumen_cifra, resumen.aciertos, resumen.respondidos),
                            style = EstiloDeCodigo.copy(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
                            color = SobreHero,
                        )
                        Text(stringResource(R.string.resumen_cifra_etiqueta), style = MaterialTheme.typography.labelMedium, color = SobreHeroSuave)
                    }
                }
                Text(stringResource(mensaje), style = MaterialTheme.typography.bodyMedium, color = SobreHeroSuave, textAlign = TextAlign.Center)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DatoDelResumen(Icons.Rounded.Bolt, stringResource(R.string.dojo_resumen_puntos, resumen.puntosSumados), FuegoDeRacha)
            if (resumen.cinturonNivel != null && resumen.cinturonPuntos != null) {
                val nombre = nombreDeCinturon(resumen.cinturonNivel)?.let { stringResource(it) } ?: resumen.cinturonNivel.toString()
                DatoDelResumen(Icons.Rounded.MilitaryTech, stringResource(R.string.resumen_cinturon, nombre, resumen.cinturonPuntos), c.acento)
            }
            // CA-56.2: los temas donde más falló, del que más al que menos; con una sesión perfecta no hay ninguno.
            if (resumen.temasMasFallados.isNotEmpty()) {
                TarjetaMC(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = c.acento, modifier = Modifier.size(22.dp))
                            Text(
                                stringResource(R.string.resumen_temas_titulo),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { heading() },
                            )
                        }
                        for (tema in resumen.temasMasFallados) {
                            val texto = if (tema.fallos == 1) {
                                stringResource(R.string.resumen_tema_fallos_uno, tema.tema)
                            } else {
                                stringResource(R.string.resumen_tema_fallos_otros, tema.tema, tema.fallos)
                            }
                            InsigniaMC(texto, tono = TonoDeInsignia.Neutro)
                        }
                    }
                }
            }
            BotonPrincipal(stringResource(R.string.dojo_volver), alVolver, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
}

// Una fila del resumen: ícono en un círculo suave y una frase.
@Composable
private fun DatoDelResumen(icono: ImageVector, texto: String, colorDelIcono: Color) {
    val c = MC.colores
    TarjetaMC(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(modifier = Modifier.size(44.dp).background(c.fondoSuave, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icono, contentDescription = null, tint = colorDelIcono, modifier = Modifier.size(24.dp))
            }
            Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
    }
}
