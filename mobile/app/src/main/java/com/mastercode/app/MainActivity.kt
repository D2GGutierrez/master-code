package com.mastercode.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mastercode.app.ui.avisos.AvisosScreen
import com.mastercode.app.ui.avisos.AvisosViewModel
import com.mastercode.app.ui.cursos.MisCursosScreen
import com.mastercode.app.ui.cursos.MisCursosViewModel
import com.mastercode.app.ui.dojo.DojoScreen
import com.mastercode.app.ui.dojo.DojoViewModel
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
        val app = application as MasterCodeApp
        val authRepository = app.authRepository
        setContent {
            MasterCodeTheme {
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
                if (conSesion && enDojo) {
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
                        viewModel = viewModel(key = "avisos", factory = AvisosViewModel.factory(app.almacenDeAvisos)),
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
                                // El espacio va en la tarjeta: si no hay racha que mostrar, no queda un hueco.
                                modifier = Modifier.padding(bottom = 12.dp),
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
