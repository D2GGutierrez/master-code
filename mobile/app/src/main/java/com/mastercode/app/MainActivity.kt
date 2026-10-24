package com.mastercode.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mastercode.app.ui.cursos.MisCursosScreen
import com.mastercode.app.ui.cursos.MisCursosViewModel
import com.mastercode.app.ui.login.LoginScreen
import com.mastercode.app.ui.login.LoginViewModel
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
                if (conSesion) {
                    MisCursosScreen(
                        viewModel = viewModel(
                            key = "mis-cursos-$sesion",
                            factory = MisCursosViewModel.factory(app.misCursosRepository),
                        ),
                        // Pendiente (HU29): todavía no hay pantalla del curso a la cual ir.
                        alAbrirCurso = {},
                        alVencerSesion = {
                            sesion++
                            conSesion = false
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
