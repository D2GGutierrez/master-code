package com.mastercode.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mastercode.app.ui.inicio.InicioScreen
import com.mastercode.app.ui.theme.MasterCodeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MasterCodeTheme {
                InicioScreen()
            }
        }
    }
}
