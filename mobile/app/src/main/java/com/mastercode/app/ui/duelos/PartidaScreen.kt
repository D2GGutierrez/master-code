package com.mastercode.app.ui.duelos

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R
import com.mastercode.app.data.duelos.JugadorDelMarcador
import com.mastercode.app.data.duelos.Marcador
import com.mastercode.app.ui.componentes.AvatarMC
import com.mastercode.app.ui.componentes.BarraDeProgreso
import com.mastercode.app.ui.componentes.BotonDeTextoSobreHero
import com.mastercode.app.ui.componentes.BotonPrincipal
import com.mastercode.app.ui.componentes.CabeceraMC
import com.mastercode.app.ui.componentes.EstadoVacio
import com.mastercode.app.ui.componentes.HeroMC
import com.mastercode.app.ui.componentes.MensajeDeError
import com.mastercode.app.ui.componentes.PantallaMC
import com.mastercode.app.ui.componentes.TarjetaMC
import com.mastercode.app.ui.ejercicios.CompletarEspacioScreen
import com.mastercode.app.ui.ejercicios.EstadoDeEnvio
import com.mastercode.app.ui.ejercicios.OpcionMultipleScreen
import com.mastercode.app.ui.ejercicios.PredecirSalidaScreen
import com.mastercode.app.ui.ejercicios.TIPO_COMPLETAR_ESPACIO
import com.mastercode.app.ui.ejercicios.TIPO_OPCION_MULTIPLE
import com.mastercode.app.ui.ejercicios.TIPO_PREDICCION_SALIDA
import com.mastercode.app.ui.theme.BordeSobreHero
import com.mastercode.app.ui.theme.CristalSobreHero
import com.mastercode.app.ui.theme.ErrorSobreHero
import com.mastercode.app.ui.theme.EstiloDeCodigo
import com.mastercode.app.ui.theme.ExitoSobreHero
import com.mastercode.app.ui.theme.MC
import com.mastercode.app.ui.theme.SobreHero
import com.mastercode.app.ui.theme.SobreHeroSuave

// H8 y HU59-T3: la pantalla de la partida. En la cabecera oscura va el marcador en vivo (puntaje, ejercicios respondidos y si
// el rival está conectado), que se actualiza solo cada 2 s; abajo, el ejercicio del turno con las mismas pantallas de
// ejercicio del resto de la app. Al responder se ve la corrección y «Siguiente ejercicio»; al terminar, el resultado a pantalla
// completa. Salir en mitad de una partida pide confirmar: user-api da la partida por abandonada si el estudiante no vuelve
// en 30 s.
@Composable
fun PartidaScreen(viewModel: PartidaViewModel, alVolver: () -> Unit, alVencerSesion: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmandoSalida by rememberSaveable { mutableStateOf(false) }

    val fase = estado.fase
    val marcador = estado.marcador
    // Solo se confirma al salir si la partida está en juego.
    val enJuego = marcador?.estado == "en_curso" && fase !is FaseDePartida.Terminada && fase !is FaseDePartida.Cerrada
    val salir = {
        if (enJuego) {
            confirmandoSalida = true
        } else {
            alVolver()
        }
    }

    BackHandler(onBack = salir)
    // El marcador se consulta solo mientras la pantalla está a la vista: al salir o pasar la app a segundo plano se deja de
    // dar señal, y user-api da la partida por abandonada si no se vuelve en 30 s.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.reanudar() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.pausar() }
    DisposableEffect(viewModel) { onDispose { viewModel.pausar() } }
    LaunchedEffect(fase) {
        if (fase == FaseDePartida.SesionVencida) alVencerSesion()
    }

    if (fase is FaseDePartida.Terminada) {
        ResultadoDeLaPartida(fase.marcador, alVolver)
    } else {
        PantallaMC(
            cabecera = {
                CabeceraMC(
                    titulo = stringResource(R.string.partida_titulo, marcador?.rival?.alias ?: ""),
                    sobretitulo = stringResource(R.string.partida_sobretitulo),
                    acciones = { BotonDeTextoSobreHero(stringResource(R.string.partida_salir), salir) },
                    extra = {
                        if (marcador != null && fase !is FaseDePartida.Cerrada && fase != FaseDePartida.EsperandoRival) {
                            MarcadorEnVivo(marcador)
                        }
                    },
                )
            },
        ) {
            if (estado.sinSenal) {
                MensajeDeError(
                    stringResource(R.string.partida_sin_senal),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (fase) {
                    FaseDePartida.Cargando, FaseDePartida.SesionVencida -> CentradoPartida {
                        val descripcion = stringResource(R.string.partida_cargando)
                        CircularProgressIndicator(
                            color = MC.colores.acento,
                            modifier = Modifier.semantics { contentDescription = descripcion },
                        )
                    }
                    FaseDePartida.EsperandoRival -> EsperandoAlOtro(
                        alias = marcador?.rival?.alias ?: "",
                        texto = stringResource(R.string.partida_esperando_rival, marcador?.rival?.alias ?: ""),
                    )
                    is FaseDePartida.Jugando -> EjercicioDeLaPartida(
                        fase = fase,
                        finDeLaPartida = marcador?.estado == "terminada",
                        alSiguiente = viewModel::siguiente,
                        alVencerSesion = alVencerSesion,
                    )
                    FaseDePartida.EsperandoAlRival -> EsperandoAlOtro(
                        alias = marcador?.rival?.alias ?: "",
                        texto = stringResource(R.string.partida_esperando_al_rival, marcador?.rival?.alias ?: ""),
                    )
                    is FaseDePartida.Terminada -> Unit // ya se mostró arriba, a pantalla completa
                    is FaseDePartida.Cerrada -> CentradoPartida {
                        EstadoVacio(
                            ilustracion = R.drawable.ilu_vacio_invitaciones,
                            texto = stringResource(fase.mensaje),
                            accion = {
                                BotonPrincipal(stringResource(R.string.partida_volver), alVolver, modifier = Modifier.fillMaxWidth())
                            },
                        )
                    }
                    is FaseDePartida.Error -> CentradoPartida {
                        EstadoVacio(
                            ilustracion = R.drawable.ilu_sin_conexion,
                            texto = stringResource(fase.mensaje),
                            accion = {
                                BotonPrincipal(stringResource(R.string.partida_reintentar), viewModel::reintentar, modifier = Modifier.fillMaxWidth())
                            },
                        )
                    }
                }
            }
        }
    }

    if (confirmandoSalida) {
        AlertDialog(
            onDismissRequest = { confirmandoSalida = false },
            title = { Text(stringResource(R.string.partida_salir_titulo)) },
            text = { Text(stringResource(R.string.partida_salir_texto)) },
            confirmButton = {
                TextButton(onClick = { confirmandoSalida = false; alVolver() }) { Text(stringResource(R.string.partida_salir_confirmar)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoSalida = false }) { Text(stringResource(R.string.partida_salir_cancelar)) }
            },
        )
    }
}

// HU59-T3: tu puntaje contra el del rival, con los ejercicios respondidos de cada uno y si el rival tiene señal. Va en vidrio
// sobre la cabecera. Se anuncia a los lectores de pantalla cada vez que cambia.
@Composable
private fun MarcadorEnVivo(marcador: Marcador) {
    Column(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .background(CristalSobreHero, RoundedCornerShape(8.dp))
            .border(1.dp, BordeSobreHero, RoundedCornerShape(8.dp))
            .padding(16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            JugadorDelMarcadorVista(stringResource(R.string.partida_tu_alias), marcador.yo, marcador.totalEjercicios, Alignment.Start, Modifier.weight(1f))
            Text(
                stringResource(R.string.partida_vs),
                style = MaterialTheme.typography.labelLarge,
                color = SobreHeroSuave,
                modifier = Modifier.padding(top = 10.dp),
            )
            JugadorDelMarcadorVista(marcador.rival.alias, marcador.rival, marcador.totalEjercicios, Alignment.End, Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // El puntito solo acompaña al texto: lo que informa es el texto, no el color.
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (marcador.rivalConectado) ExitoSobreHero else ErrorSobreHero, CircleShape),
            )
            Text(
                stringResource(if (marcador.rivalConectado) R.string.partida_rival_conectado else R.string.partida_rival_sin_senal, marcador.rival.alias),
                style = MaterialTheme.typography.bodySmall,
                color = if (marcador.rivalConectado) SobreHeroSuave else ErrorSobreHero,
            )
        }
    }
}

@Composable
private fun JugadorDelMarcadorVista(
    nombre: String,
    jugador: JugadorDelMarcador,
    total: Int,
    alineacion: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = alineacion, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(nombre, style = MaterialTheme.typography.titleSmall, color = SobreHeroSuave, maxLines = 1)
        Text(
            jugador.puntaje.toString(),
            style = EstiloDeCodigo.copy(fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
            color = SobreHero,
        )
        val respondidos = stringResource(R.string.partida_respondidos, jugador.respondidos, total)
        BarraDeProgreso(
            progreso = if (total > 0) jugador.respondidos.toFloat() / total else 0f,
            descripcion = respondidos,
            alto = 6.dp,
            colorDeLaPista = BordeSobreHero,
        )
        Text(respondidos, style = MaterialTheme.typography.bodySmall, color = SobreHeroSuave)
    }
}

// El ejercicio del turno con las pantallas de ejercicio de siempre. Observa el estado del propio ejercicio para mostrar
// «Siguiente ejercicio» (o «Ver resultado» si la partida ya terminó) en cuanto está corregido.
@Composable
private fun EjercicioDeLaPartida(
    fase: FaseDePartida.Jugando,
    finDeLaPartida: Boolean,
    alSiguiente: () -> Unit,
    alVencerSesion: () -> Unit,
) {
    val c = MC.colores
    val ejercicio = fase.ejercicio
    val estadoDelEjercicio by ejercicio.uiState.collectAsStateWithLifecycle()
    val respondido = estadoDelEjercicio.envio is EstadoDeEnvio.Resultado
    val turno = stringResource(R.string.partida_turno, fase.turno, fase.total)

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(turno, style = MaterialTheme.typography.labelLarge, color = c.textoSuave)
            // Avanza al pasar de ejercicio y se llena el turno actual cuando ya se respondió.
            BarraDeProgreso(
                progreso = (fase.turno - 1 + if (respondido) 1 else 0).toFloat() / fase.total.coerceAtLeast(1),
                descripcion = turno,
                alto = 6.dp,
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (fase.tipo) {
                TIPO_PREDICCION_SALIDA -> PredecirSalidaScreen(ejercicio, alVencerSesion)
                TIPO_COMPLETAR_ESPACIO -> CompletarEspacioScreen(ejercicio, alVencerSesion)
                TIPO_OPCION_MULTIPLE -> OpcionMultipleScreen(ejercicio, alVencerSesion)
                else -> CentradoPartida { Text(stringResource(R.string.partida_tipo_no_soportado), textAlign = TextAlign.Center, color = c.textoSuave) }
            }
        }
        if (respondido) {
            Surface(color = c.fondo, border = BorderStroke(1.dp, c.borde), modifier = Modifier.fillMaxWidth()) {
                BotonPrincipal(
                    texto = stringResource(if (finDeLaPartida) R.string.partida_ver_resultado else R.string.partida_siguiente),
                    alTocar = alSiguiente,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

// Mientras el rival acepta el reto o termina sus ejercicios: su avatar, un mensaje y una ruedita.
@Composable
private fun EsperandoAlOtro(alias: String, texto: String) {
    CentradoPartida {
        AvatarMC(alias, tamano = 72.dp)
        Text(
            texto,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MC.colores.textoSuave,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        CircularProgressIndicator(color = MC.colores.acento, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
    }
}

// El final de la partida, a pantalla completa: quién ganó, por qué (si fue por abandono) y el marcador final.
@Composable
private fun ResultadoDeLaPartida(marcador: Marcador, alVolver: () -> Unit) {
    val c = MC.colores
    val texto = textoFinalDe(marcador)
    @DrawableRes
    val ilustracion: Int = when (marcador.resultado) {
        "gane" -> R.drawable.ilu_duelo_victoria
        "perdi" -> R.drawable.ilu_duelo_derrota
        else -> R.drawable.ilu_duelo_empate
    }
    val final = stringResource(R.string.partida_final_puntaje, marcador.yo.puntaje, marcador.rival.puntaje)
    PantallaMC(
        cabecera = {
            HeroMC {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Image(painter = painterResource(ilustracion), contentDescription = null, modifier = Modifier.height(132.dp))
                    Text(
                        stringResource(texto.titulo),
                        style = MaterialTheme.typography.headlineLarge,
                        color = SobreHero,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    texto.motivo?.let {
                        Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = SobreHeroSuave, textAlign = TextAlign.Center)
                    }
                }
            }
        },
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TarjetaMC(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp).semantics(mergeDescendants = true) { contentDescription = final },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        stringResource(R.string.partida_titulo, marcador.rival.alias),
                        style = MaterialTheme.typography.labelLarge,
                        color = c.textoSuave,
                    )
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        PuntajeFinal(stringResource(R.string.partida_tu_alias), marcador.yo.puntaje, Modifier.weight(1f))
                        Text(stringResource(R.string.partida_vs), style = MaterialTheme.typography.labelLarge, color = c.textoSuave)
                        PuntajeFinal(marcador.rival.alias, marcador.rival.puntaje, Modifier.weight(1f))
                    }
                    Text(final, style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
                }
            }
            BotonPrincipal(stringResource(R.string.partida_volver), alVolver, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PuntajeFinal(nombre: String, puntaje: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(puntaje.toString(), style = EstiloDeCodigo.copy(fontSize = 44.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold))
        Text(nombre, style = MaterialTheme.typography.titleSmall, color = MC.colores.textoSuave, maxLines = 1)
    }
}

@Composable
private fun CentradoPartida(contenido: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) { contenido() }
    }
}
