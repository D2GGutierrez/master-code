package com.mastercode.app.ui.dojo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mastercode.app.R
import com.mastercode.app.data.dojo.ResumenDeSesion
import com.mastercode.app.ui.cinturon.nombreDeCinturon
import com.mastercode.app.ui.componentes.AnilloDeProgreso
import com.mastercode.app.ui.componentes.BannerMC
import com.mastercode.app.ui.componentes.BotonPrincipal
import com.mastercode.app.ui.componentes.CabeceraMC
import com.mastercode.app.ui.componentes.EtiquetaMC
import com.mastercode.app.ui.componentes.InsigniaMC
import com.mastercode.app.ui.componentes.PantallaMC
import com.mastercode.app.ui.componentes.TarjetaDorada
import com.mastercode.app.ui.componentes.TarjetaMC
import com.mastercode.app.ui.componentes.TonoDeInsignia
import com.mastercode.app.ui.perfil.imagenDelCinturon
import com.mastercode.app.ui.theme.Dorado
import com.mastercode.app.ui.theme.EstiloDeCodigo
import com.mastercode.app.ui.theme.MC
import com.mastercode.app.ui.componentes.ConfetiMC
import com.mastercode.app.ui.componentes.contadorAnimado
import com.mastercode.app.ui.componentes.entrada
import androidx.compose.foundation.layout.fillMaxSize
import com.mastercode.app.ui.componentes.resplandor
import com.mastercode.app.ui.componentes.VentanaMC

// HU56-T2 (CA-56.1, CA-56.2, CA-56.4, CA-56.5): el resumen al terminar la sesión del Dojo: aciertos de respondidos, puntos
// sumados, el cinturón y los temas a repasar. Cubre la sesión sin errores (sin temas) y la sesión sin aciertos. Con el estilo de
// Consoly: el anillo de aciertos, la tarjeta dorada del desglose de puntos y el banner con el mensaje.
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
    // Los números suben desde cero al abrir el resumen, y con aciertos cae confeti.
    val aciertosAnimados = contadorAnimado(resumen.aciertos, 900)
    val puntosAnimados = contadorAnimado(resumen.puntosSumados, 1100)
    val totalAnimado = contadorAnimado(resumen.cinturonPuntos ?: 0, 1300)
    Box(modifier = Modifier.fillMaxSize()) {
        PantallaMC(cabecera = { CabeceraMC(titulo = stringResource(R.string.dojo_resumen_titulo)) }, conAurora = true) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // El anillo: aciertos de respondidos. Para el lector de pantalla se dice con la frase completa.
                Box(modifier = Modifier.fillMaxWidth().entrada(0), contentAlignment = Alignment.Center) {
                    val fraccion = if (resumen.respondidos > 0) resumen.aciertos.toFloat() / resumen.respondidos else 0f
                    AnilloDeProgreso(
                        progreso = fraccion,
                        tamano = 148.dp,
                        grosor = 12.dp,
                        colorDelArco = c.acento,
                        modifier = Modifier.resplandor(c.acento, 74.dp, intensidad = 0.7f).semantics { contentDescription = aciertosEnTexto },
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.resumen_cifra, aciertosAnimados, resumen.respondidos),
                                style = EstiloDeCodigo.copy(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
                                color = c.texto,
                            )
                            EtiquetaMC(stringResource(R.string.resumen_cifra_etiqueta))
                        }
                    }
                }
                // El desglose de puntos: aciertos, puntos de esta sesión y, si se sabe, el total del cinturón.
                TarjetaDorada(modifier = Modifier.fillMaxWidth().entrada(120)) {
                    EtiquetaMC(stringResource(R.string.resumen_desglose), color = Dorado, modifier = Modifier.semantics { heading() })
                    FilaDelDesglose(stringResource(R.string.resumen_aciertos_etiqueta), stringResource(R.string.resumen_cifra, resumen.aciertos, resumen.respondidos))
                    FilaDelDesglose(stringResource(R.string.resumen_puntos_etiqueta), stringResource(R.string.resumen_puntos_valor, puntosAnimados))
                    if (resumen.cinturonPuntos != null) {
                        Box(modifier = Modifier.padding(top = 14.dp).fillMaxWidth().height(1.dp).background(Dorado.copy(alpha = 0.35f)))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            EtiquetaMC(stringResource(R.string.resumen_total), color = Dorado, modifier = Modifier.weight(1f))
                            Text(
                                totalAnimado.toString(),
                                style = EstiloDeCodigo.copy(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
                                color = Dorado,
                            )
                        }
                    }
                }
                if (resumen.cinturonNivel != null && resumen.cinturonPuntos != null) {
                    val nombre = nombreDeCinturon(resumen.cinturonNivel)?.let { stringResource(it) } ?: resumen.cinturonNivel.toString()
                    TarjetaMC(modifier = Modifier.fillMaxWidth().entrada(220)) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            val forma = RoundedCornerShape(14.dp)
                            Box(
                                modifier = Modifier.size(68.dp).background(c.lienzo, forma).border(1.dp, c.borde, forma),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(painter = painterResource(imagenDelCinturon(resumen.cinturonNivel)), contentDescription = null, modifier = Modifier.size(56.dp))
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                EtiquetaMC(stringResource(R.string.resumen_cinturon_etiqueta))
                                Text(
                                    stringResource(R.string.resumen_cinturon, nombre, resumen.cinturonPuntos),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }
                // CA-56.2: los temas donde más falló, del que más al que menos; con una sesión perfecta no hay ninguno.
                if (resumen.temasMasFallados.isNotEmpty()) {
                    VentanaMC(archivo = "temas_para_repasar.log", modifier = Modifier.fillMaxWidth().entrada(300)) {
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
                BannerMC(stringResource(mensaje), Modifier.entrada(380))
                BotonPrincipal(stringResource(R.string.dojo_volver), alVolver, modifier = Modifier.fillMaxWidth().entrada(440))
            }
        }
        if (resumen.aciertos > 0) ConfetiMC(modifier = Modifier.fillMaxSize())
    }
}

// Una fila del desglose: la etiqueta a la izquierda y el valor, en letra monoespaciada, a la derecha.
@Composable
private fun FilaDelDesglose(etiqueta: String, valor: String) {
    val c = MC.colores
    Row(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, style = MaterialTheme.typography.bodyLarge, color = c.texto, modifier = Modifier.weight(1f))
        Text(valor, style = EstiloDeCodigo.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold), color = c.texto)
    }
}
