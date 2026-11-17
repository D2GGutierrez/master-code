package com.mastercode.app

import android.app.Application
import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.HttpAuthApi
import com.mastercode.app.data.auth.KeystoreTokenStorage
import com.mastercode.app.data.cursos.HttpMisCursosApi
import com.mastercode.app.data.cursos.MisCursosRepository
import com.mastercode.app.data.dojo.DojoRepository
import com.mastercode.app.data.dojo.HttpDojoApi
import com.mastercode.app.data.problemas.HttpProblemaApi
import com.mastercode.app.data.problemas.ProblemaRepository
import com.mastercode.app.data.red.MonitorDeRed
import com.mastercode.app.data.respuestas.EnviadorDeRespuestas
import com.mastercode.app.data.respuestas.PreferenciasDeSesion
import com.mastercode.app.data.respuestas.PreferenciasPendientes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Crea las dependencias a mano, una sola vez por proceso: así el access token en memoria
// no se pierde cuando la Activity se recrea (por ejemplo, al girar el teléfono).
class MasterCodeApp : Application() {
    val authRepository by lazy {
        AuthRepository(HttpAuthApi(BuildConfig.ADMIN_API_URL), KeystoreTokenStorage(this))
    }
    val misCursosRepository by lazy {
        MisCursosRepository(HttpMisCursosApi(BuildConfig.USER_API_URL), authRepository)
    }
    // Lo usan las pantallas de ejercicio a las que lleva el Dojo (HU53-T3).
    val problemaRepository by lazy {
        ProblemaRepository(HttpProblemaApi(BuildConfig.USER_API_URL), authRepository)
    }
    val dojoRepository by lazy {
        DojoRepository(HttpDojoApi(BuildConfig.USER_API_URL), authRepository)
    }
    // HU48-T3: envía las respuestas y guarda en el teléfono las que no pudo enviar.
    val enviadorDeRespuestas by lazy {
        EnviadorDeRespuestas(problemaRepository, PreferenciasPendientes(this))
    }
    // HU55-T1: dónde queda guardado el estado de la sesión del Dojo.
    val almacenDeSesion by lazy { PreferenciasDeSesion(this) }

    // Los reenvíos viven con la app, no con una pantalla: si el estudiante sale, el reenvío sigue.
    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // HU48-T3 (CA-48.4): se reenvía lo guardado al abrir la app y cada vez que vuelve la señal.
        reenviarRespuestasPendientes()
        MonitorDeRed(this, ::reenviarRespuestasPendientes).iniciar()
    }

    private fun reenviarRespuestasPendientes() {
        ambito.launch { enviadorDeRespuestas.reenviarPendientes() }
    }
}
