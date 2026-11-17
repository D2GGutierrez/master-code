package com.mastercode.app.data.respuestas

import android.content.Context
import com.mastercode.app.data.problemas.RespuestaDelEstudiante

// Las dos implementaciones reales de AlmacenLocal: SharedPreferences privadas de la app. Nada de esto es secreto
// (respuestas a ejercicios y un contador), por eso no pasa por Keystore como el refresh token.
private const val ARCHIVO = "master_code_local"

class PreferenciasPendientes(contexto: Context) : AlmacenDePendientes {
    private val preferencias = contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    override fun leer(): List<RespuestaDelEstudiante> = respuestasDesdeJson(preferencias.getString(CLAVE, null))

    override fun guardar(lista: List<RespuestaDelEstudiante>) {
        preferencias.edit().putString(CLAVE, lista.aJson()).apply()
    }

    private companion object {
        const val CLAVE = "respuestas_pendientes"
    }
}

class PreferenciasDeSesion(contexto: Context) : AlmacenDeSesion {
    private val preferencias = contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    override fun leer(): EstadoDeSesion? = estadoDeSesionDesdeJson(preferencias.getString(CLAVE, null))

    override fun guardar(estado: EstadoDeSesion?) {
        val editor = preferencias.edit()
        if (estado == null) editor.remove(CLAVE) else editor.putString(CLAVE, estado.aJson())
        editor.apply()
    }

    private companion object {
        const val CLAVE = "sesion_dojo"
    }
}
