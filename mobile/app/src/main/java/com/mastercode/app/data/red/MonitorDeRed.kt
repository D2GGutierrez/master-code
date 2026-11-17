package com.mastercode.app.data.red

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network

// HU48-T3: avisa cada vez que el teléfono recupera una conexión, para reenviar las respuestas guardadas sin que el
// estudiante tenga que hacer nada. Necesita el permiso normal ACCESS_NETWORK_STATE (se concede al instalar). Se
// registra una vez por proceso y vive con la app, por eso no se desregistra.
class MonitorDeRed(contexto: Context, private val alVolverLaSenal: () -> Unit) {
    private val conectividad = contexto.getSystemService(ConnectivityManager::class.java)

    fun iniciar() {
        conectividad.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            // También se llama al registrarse si ya hay red: reenviar de más es inofensivo (no hay nada pendiente o el
            // reenvío ya está en curso).
            override fun onAvailable(network: Network) {
                alVolverLaSenal()
            }
        })
    }
}
