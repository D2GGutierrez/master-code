package com.mastercode.app.data.dispositivo

import android.content.Context
import com.mastercode.app.data.avisos.AjustesDeAvisos
import com.mastercode.app.data.avisos.AlmacenDeAvisos

// Las implementaciones reales: SharedPreferences privadas de la app. El token de Firebase y los ajustes de avisos no son
// secretos de la cuenta (el token solo sirve para mandar avisos a este teléfono, y user-api lo guarda de todos modos).
private const val ARCHIVO = "master_code_local"

class PreferenciasDeToken(contexto: Context) : AlmacenDeToken {
    private val preferencias = contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    override fun leerPendiente(): String? = preferencias.getString(CLAVE, null)

    override fun guardarPendiente(token: String?) {
        val editor = preferencias.edit()
        if (token == null) editor.remove(CLAVE) else editor.putString(CLAVE, token)
        editor.apply()
    }

    private companion object {
        const val CLAVE = "token_fcm_pendiente"
    }
}

class PreferenciasDeAvisos(contexto: Context) : AlmacenDeAvisos {
    private val preferencias = contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    override fun leer(): AjustesDeAvisos {
        val hora = preferencias.getInt(HORA, AjustesDeAvisos.HORA_POR_DEFECTO)
        val minuto = preferencias.getInt(MINUTO, AjustesDeAvisos.MINUTO_POR_DEFECTO)
        // Una hora ilegible vuelve a la de por defecto en vez de dejar la pantalla con valores imposibles.
        val valida = AjustesDeAvisos.horaValida(hora, minuto)
        return AjustesDeAvisos(
            activados = preferencias.getBoolean(ACTIVADOS, true),
            hora = if (valida) hora else AjustesDeAvisos.HORA_POR_DEFECTO,
            minuto = if (valida) minuto else AjustesDeAvisos.MINUTO_POR_DEFECTO,
        )
    }

    override fun guardar(ajustes: AjustesDeAvisos) {
        preferencias.edit()
            .putBoolean(ACTIVADOS, ajustes.activados)
            .putInt(HORA, ajustes.hora)
            .putInt(MINUTO, ajustes.minuto)
            .apply()
    }

    private companion object {
        const val ACTIVADOS = "avisos_activados"
        const val HORA = "avisos_hora"
        const val MINUTO = "avisos_minuto"
    }
}
