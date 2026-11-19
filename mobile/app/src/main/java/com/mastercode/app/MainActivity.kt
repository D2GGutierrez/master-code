package com.mastercode.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mastercode.app.ui.avisos.AvisosScreen
import com.mastercode.app.ui.avisos.AvisosViewModel
import com.mastercode.app.ui.componentes.BarraInferiorMC
import com.mastercode.app.ui.componentes.DestinoMC
import com.mastercode.app.ui.cursos.MisCursosScreen
import com.mastercode.app.ui.cursos.MisCursosViewModel
import com.mastercode.app.ui.diario.DiarioScreen
import com.mastercode.app.ui.dojo.DojoScreen
import com.mastercode.app.ui.dojo.DojoViewModel
import com.mastercode.app.ui.duelos.DuelosScreen
import com.mastercode.app.ui.duelos.PartidaScreen
import com.mastercode.app.ui.duelos.PartidaViewModel
import com.mastercode.app.ui.login.LoginScreen
import com.mastercode.app.ui.login.LoginViewModel
import com.mastercode.app.ui.perfil.PerfilScreen
import com.mastercode.app.ui.perfil.PerfilViewModel
import com.mastercode.app.ui.racha.RachaViewModel
import com.mastercode.app.ui.theme.MasterCodeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // De borde a borde: la app dibuja bajo las barras del sistema y cada pantalla se aparta de ellas con los insets. El tema
        // es siempre oscuro (como Consoly), así que los íconos de las dos barras van siempre claros.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val app = application as MasterCodeApp
        val authRepository = app.authRepository
        setContent {
            MasterCodeTheme {
                // La superficie de fondo hace que el texto sin color propio use el del tema.
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Sin librería de navegación: con refresh guardado se va al Diario; si no, al login.
                    var conSesion by rememberSaveable { mutableStateOf(authRepository.haySesionGuardada()) }
                    // Cambia cada vez que la sesión vence: así login y pestañas empiezan con ViewModels nuevos, y el login no
                    // reusa un sesionIniciada=true de la sesión anterior.
                    var sesion by rememberSaveable { mutableIntStateOf(0) }
                    // La barra inferior tiene cinco pestañas (posición en DestinoMC): Diario, Dojo, Batallas, Cursos y Perfil.
                    var pestana by rememberSaveable { mutableIntStateOf(DestinoMC.Diario.ordinal) }
                    // Cada vez que se entra a una pestaña con ViewModel propio se usa uno nuevo: el Dojo empieza una sesión limpia,
                    // los duelos y el perfil traen datos frescos (HU53-T3, HU57-T3, HU51-T2).
                    var visitaAlDojo by rememberSaveable { mutableIntStateOf(0) }
                    var visitaADuelos by rememberSaveable { mutableIntStateOf(0) }
                    var visitaAlPerfil by rememberSaveable { mutableIntStateOf(0) }
                    // La racha del Diario se vuelve a pedir cada vez que se llega a él (así cuenta lo que se practicó en el Dojo).
                    // Cambia al llegar, no al salir: durante el fundido entre pestañas el Diario que se va conserva su racha.
                    var visitaARacha by rememberSaveable { mutableIntStateOf(0) }
                    // HU52-T3: los avisos de práctica, que se abren desde el Diario y el Perfil y tapan la barra.
                    var enAvisos by rememberSaveable { mutableStateOf(false) }
                    // H8 y HU60-T3: desde Batallas se abre una partida (partidaAbierta es su id, o -1 si no hay ninguna). Al volver
                    // de ella se renuevan las pestañas de duelos, porque una partida terminada cambia el historial, el ranking y
                    // las invitaciones.
                    var partidaAbierta by rememberSaveable { mutableLongStateOf(-1L) }
                    // Cada vez que se abre una partida (aunque sea la misma) empieza con un ViewModel nuevo y su propio sondeo.
                    var visitaALaPartida by rememberSaveable { mutableIntStateOf(0) }

                    fun irA(destino: DestinoMC) {
                        if (destino.ordinal == pestana) return
                        when (destino) {
                            DestinoMC.Dojo -> visitaAlDojo++
                            DestinoMC.Batallas -> visitaADuelos++
                            DestinoMC.Perfil -> visitaAlPerfil++
                            DestinoMC.Diario -> visitaARacha++
                            else -> Unit
                        }
                        pestana = destino.ordinal
                    }

                    fun volverAlDiario() {
                        visitaARacha++
                        pestana = DestinoMC.Diario.ordinal
                    }

                    fun vencerSesion() {
                        sesion++
                        pestana = DestinoMC.Diario.ordinal
                        partidaAbierta = -1L
                        enAvisos = false
                        conSesion = false
                    }

                    // H6: con la sesión iniciada se le dice a user-api la zona horaria del teléfono (también al volver a iniciar
                    // sesión). Si falla, no se avisa: se reintenta la próxima vez que se abra la app.
                    LaunchedEffect(conSesion) {
                        if (conSesion) app.zonaHorariaRepository.enviar()
                    }
                    // HU52-T1: con la sesión iniciada se registra en user-api el token de Firebase del teléfono (sin Firebase
                    // todavía no hay token y no se manda nada). Un fallo se reintenta la próxima vez.
                    LaunchedEffect(conSesion) {
                        if (conSesion) app.dispositivoRepository.registrarElDelTelefono()
                    }
                    // HU52-T4: y los ajustes de avisos, para que el servidor mande el push a la hora elegida. Si un cambio no llegó
                    // por falta de señal, llega aquí la próxima vez.
                    LaunchedEffect(conSesion) {
                        if (conSesion) app.enviarAvisos(app.almacenDeAvisos.leer())
                    }
                    // Atrás desde Cursos o Perfil lleva al Diario. Dojo y Batallas tienen su propio manejo de «atrás».
                    BackHandler(
                        enabled = conSesion && partidaAbierta < 0 && !enAvisos &&
                            (pestana == DestinoMC.Cursos.ordinal || pestana == DestinoMC.Perfil.ordinal),
                    ) { volverAlDiario() }

                    // La barra de navegación inferior, común a las cinco pestañas.
                    val barra: @Composable () -> Unit = {
                        BarraInferiorMC(seleccion = DestinoMC.values()[pestana], alElegir = { irA(it) })
                    }

                    if (conSesion && partidaAbierta >= 0) {
                        PartidaScreen(
                            viewModel = viewModel(
                                key = "partida-$sesion-$partidaAbierta-$visitaALaPartida",
                                factory = PartidaViewModel.factory(app.duelosRepository, partidaAbierta),
                            ),
                            alVolver = {
                                partidaAbierta = -1L
                                visitaADuelos++
                            },
                            alVencerSesion = { vencerSesion() },
                        )
                    } else if (conSesion && enAvisos) {
                        AvisosScreen(
                            viewModel = viewModel(key = "avisos", factory = AvisosViewModel.factory(app.almacenDeAvisos, app::enviarAvisos)),
                            alVolver = { enAvisos = false },
                        )
                    } else if (conSesion) {
                        // Entre pestañas, un fundido corto en lugar de un corte seco.
                        Crossfade(targetState = pestana, animationSpec = tween(durationMillis = 220), label = "pestana") { actual ->
                        when (DestinoMC.values()[actual]) {
                            DestinoMC.Diario -> DiarioScreen(
                                // HU49-T3: la racha. La clave cambia al llegar al Diario para que se vuelva a pedir y cuente lo practicado.
                                viewModel = viewModel(
                                    key = "racha-$sesion-$visitaARacha",
                                    factory = RachaViewModel.factory(app.rachaRepository),
                                ),
                                alIniciarPractica = { irA(DestinoMC.Dojo) },
                                alAbrirBatallas = { irA(DestinoMC.Batallas) },
                                alAbrirAvisos = { enAvisos = true },
                                pie = barra,
                            )
                            DestinoMC.Dojo -> DojoScreen(
                                viewModel = viewModel(
                                    key = "dojo-$sesion-$visitaAlDojo",
                                    factory = DojoViewModel.factory(app.dojoRepository, app.almacenDeSesion),
                                ),
                                problemaRepository = app.problemaRepository,
                                enviador = app.enviadorDeRespuestas,
                                alVolver = { volverAlDiario() },
                                alVencerSesion = { vencerSesion() },
                                barra = barra,
                            )
                            DestinoMC.Batallas -> DuelosScreen(
                                misCursos = app.misCursosRepository,
                                duelos = app.duelosRepository,
                                visita = "$sesion-$visitaADuelos",
                                alAbrirPartida = {
                                    visitaALaPartida++
                                    partidaAbierta = it
                                },
                                alVolver = { volverAlDiario() },
                                alVencerSesion = { vencerSesion() },
                                barra = barra,
                            )
                            DestinoMC.Cursos -> MisCursosScreen(
                                viewModel = viewModel(
                                    key = "mis-cursos-$sesion",
                                    factory = MisCursosViewModel.factory(app.misCursosRepository),
                                ),
                                // Pendiente (HU29): todavía no hay pantalla del curso a la cual ir.
                                alAbrirCurso = {},
                                alVencerSesion = { vencerSesion() },
                                pie = barra,
                            )
                            DestinoMC.Perfil -> PerfilScreen(
                                viewModel = viewModel(
                                    key = "perfil-$sesion-$visitaAlPerfil",
                                    factory = PerfilViewModel.factory(app.cinturonesRepository),
                                ),
                                alAbrirAvisos = { enAvisos = true },
                                alVencerSesion = { vencerSesion() },
                                pie = barra,
                            )
                        }
                        }
                    } else {
                        LoginScreen(
                            viewModel = viewModel(key = "login-$sesion", factory = LoginViewModel.factory(authRepository)),
                            alIniciarSesion = { conSesion = true },
                        )
                    }
                }
            }
        }
    }
}
