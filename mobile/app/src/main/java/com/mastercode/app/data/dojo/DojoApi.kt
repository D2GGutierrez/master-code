package com.mastercode.app.data.dojo

import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.pedirJson
import com.mastercode.app.data.problemas.ProblemaDelEjercicio
import com.mastercode.app.data.problemas.problemaDesdeJson
import org.json.JSONException
import org.json.JSONObject

// Una sesión del Dojo recién abierta (HU53-T3): su id, el lenguaje y la dificultad con la que parte.
data class SesionIniciada(val id: Long, val lenguaje: String, val nivel: String)

// Llamadas al Dojo de user-api. Lanzan ErrorHttp si el servidor rechaza y IOException si no hay conexión.
interface DojoApi {
    suspend fun iniciar(access: String, lenguaje: String): SesionIniciada

    // null cuando ya no quedan ejercicios (204).
    suspend fun siguiente(access: String, sesionId: Long): ProblemaDelEjercicio?
}

class HttpDojoApi(private val urlBase: String) : DojoApi {

    override suspend fun iniciar(access: String, lenguaje: String): SesionIniciada {
        val respuesta = pedirJson(urlBase + "api/dojo/sesiones", access, "POST", JSONObject().put("lenguaje", lenguaje))
        if (respuesta.codigo != 201) throw ErrorHttp(respuesta.codigo)
        return try {
            val json = JSONObject(respuesta.texto)
            SesionIniciada(json.getLong("id"), json.getString("lenguaje"), json.getString("nivel"))
        } catch (e: JSONException) {
            throw ErrorHttp(respuesta.codigo)
        }
    }

    override suspend fun siguiente(access: String, sesionId: Long): ProblemaDelEjercicio? {
        val respuesta = pedirJson(urlBase + "api/dojo/sesiones/$sesionId/siguiente", access, "POST")
        if (respuesta.codigo == 204) return null
        if (respuesta.codigo != 200) throw ErrorHttp(respuesta.codigo)
        return try {
            problemaDesdeJson(JSONObject(respuesta.texto))
        } catch (e: JSONException) {
            throw ErrorHttp(respuesta.codigo)
        }
    }
}
