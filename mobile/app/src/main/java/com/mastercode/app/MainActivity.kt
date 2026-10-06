package com.mastercode.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mastercode.app.ui.inicio.InicioScreen
import com.mastercode.app.ui.login.LoginScreen
import com.mastercode.app.ui.login.LoginViewModel
import com.mastercode.app.ui.theme.MasterCodeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authRepository = (application as MasterCodeApp).authRepository
        setContent {
            MasterCodeTheme {
                // Sin librería de navegación: con refresh guardado se va a inicio; si no, al login.
                var conSesion by rememberSaveable { mutableStateOf(authRepository.haySesionGuardada()) }
                if (conSesion) {
                    InicioScreen()
                } else {
                    LoginScreen(
                        viewModel = viewModel(factory = LoginViewModel.factory(authRepository)),
                        alIniciarSesion = { conSesion = true },
                    )
                }
            }
        }
    }
}
