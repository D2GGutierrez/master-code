package com.mastercode.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.mastercode.app.ui.cursos.MisCursosScreen
import com.mastercode.app.ui.cursos.MisCursosViewModel
import com.mastercode.app.ui.dojo.DojoScreen
import com.mastercode.app.ui.dojo.DojoViewModel
import com.mastercode.app.ui.duelos.DuelosScreen
import com.mastercode.app.ui.duelos.PartidaScreen
import com.mastercode.app.ui.duelos.PartidaViewModel
import com.mastercode.app.ui.login.LoginScreen
import com.mastercode.app.ui.login.LoginViewModel
import com.mastercode.app.ui.perfil.PerfilScreen
import com.mastercode.app.ui.perfil.PerfilViewModel
import com.mastercode.app.ui.racha.RachaBanner
import com.mastercode.app.ui.racha.RachaViewModel
import com.mastercode.app.ui.theme.MasterCodeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // De borde a borde: la app dibuja bajo las barras del sistema y cada pantalla se aparta de ellas con los insets. Toda
        // pantalla abre con una cabecera oscura, así que los íconos de la barra de estado van siempre claros; los de la barra
        // de navegación siguen el tema del teléfono.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val app = application as MasterCodeApp
        val authRepository = app.authRepository
        setContent {
            MasterCodeTheme {
                // La superficie de fondo hace que el texto sin color propio use el del tema.
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Sin librería de navegación: con refresh guardado se va a «Mis cursos»; si no, al login.
                    var conSesion by rememberSaveable { mutableStateOf(authRepository.haySesionGuardada()) }
                    // Cambia cada vez que la sesión vence: así login y «Mis cursos» empiezan con ViewModels
                    // nuevos, y el login no reusa un sesionIniciada=true de la sesión anterior.
                    var sesion by rememberSaveable { mutableIntStateOf(0) }
                    // El Dojo (HU53-T3) se abre desde «Mis cursos». Cada visita usa un ViewModel nuevo: se empieza una sesión limpia.
                    var enDojo by rememberSaveable { mutableStateOf(false) }
                    var visitaAlDojo by rememberSaveable { mutableIntStateOf(0) }
                    // HU52-T3: los avisos de práctica se ajustan desde «Mis cursos».
                    var enAvisos by rememberSaveable { mutableStateOf(false) }
                    // HU57-T3, H8 y HU60-T3: la sección Duelos se abre desde «Mis cursos» y desde ella se abre una partida
                    // (partidaAbierta es su id, o -1 si no hay ninguna). Al volver de la partida se renuevan las pestañas
                    // (visitaADuelos), porque una partida terminada cambia el historial, el ranking y las invitaciones.
                    var enDuelos by rememberSaveable { mutableStateOf(false) }
                    var visitaADuelos by rememberSaveable { mutableIntStateOf(0) }
                    var partidaAbierta by rememberSaveable { mutableLongStateOf(-1L) }
                    // Cada vez que se abre una partida (aunque sea la misma) empieza con un ViewModel nuevo y su propio sondeo.
                    var visitaALaPartida by rememberSaveable { mutableIntStateOf(0) }
                    // HU51-T2: el perfil con el cinturón. Cada visita, como las del Dojo, usa un ViewModel nuevo y carga datos frescos.
                    var enPerfil by rememberSaveable { mutableStateOf(false) }
                    var visitaAlPerfil by rememberSaveable { mutableIntStateOf(0) }
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
                            alVencerSesion = {
                                sesion++
                                partidaAbierta = -1L
                                enDuelos = false
                                conSesion = false
                            },
                        )
                    } else if (conSesion && enDuelos) {
                        DuelosScreen(
                            misCursos = app.misCursosRepository,
                            duelos = app.duelosRepository,
                            visita = "$sesion-$visitaADuelos",
                            alAbrirPartida = {
                                visitaALaPartida++
                                partidaAbierta = it
                            },
                            alVolver = { enDuelos = false },
                            alVencerSesion = {
                                sesion++
                                enDuelos = false
                                conSesion = false
                            },
                        )
                    } else if (conSesion && enDojo) {
                        DojoScreen(
                            viewModel = viewModel(
                                key = "dojo-$sesion-$visitaAlDojo",
                                factory = DojoViewModel.factory(app.dojoRepository, app.almacenDeSesion),
                            ),
                            problemaRepository = app.problemaRepository,
                            enviador = app.enviadorDeRespuestas,
                            alVolver = { enDojo = false },
                            alVencerSesion = {
                                sesion++
                                enDojo = false
                                conSesion = false
                            },
                        )
                    } else if (conSesion && enPerfil) {
                        PerfilScreen(
                            viewModel = viewModel(
                                key = "perfil-$sesion-$visitaAlPerfil",
                                factory = PerfilViewModel.factory(app.cinturonesRepository),
                            ),
                            alVolver = { enPerfil = false },
                            alVencerSesion = {
                                sesion++
                                enPerfil = false
                                conSesion = false
                            },
                        )
                    } else if (conSesion && enAvisos) {
                        AvisosScreen(
                            viewModel = viewModel(key = "avisos", factory = AvisosViewModel.factory(app.almacenDeAvisos, app::enviarAvisos)),
                            alVolver = { enAvisos = false },
                        )
                    } else if (conSesion) {
                        MisCursosScreen(
                            viewModel = viewModel(
                                key = "mis-cursos-$sesion",
                                factory = MisCursosViewModel.factory(app.misCursosRepository),
                            ),
                            // Pendiente (HU29): todavía no hay pantalla del curso a la cual ir.
                            alAbrirCurso = {},
                            alAbrirDojo = {
                                visitaAlDojo++
                                enDojo = true
                            },
                            alVencerSesion = {
                                sesion++
                                conSesion = false
                            },
                            alAbrirAvisos = { enAvisos = true },
                            alAbrirDuelos = {
                                visitaADuelos++
                                enDuelos = true
                            },
                            alAbrirPerfil = {
                                visitaAlPerfil++
                                enPerfil = true
                            },
                            // HU49-T3: la racha. La clave cambia al volver del Dojo para que se vuelva a pedir y cuente lo practicado.
                            encabezado = {
                                RachaBanner(
                                    viewModel = viewModel(
                                        key = "racha-$sesion-$visitaAlDojo",
                                        factory = RachaViewModel.factory(app.rachaRepository),
                                    ),
                                )
                            },
                        )
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
