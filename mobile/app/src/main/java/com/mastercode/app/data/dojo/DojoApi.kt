package com.mastercode.app.data.dojo

import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.pedirJson
import com.mastercode.app.data.problemas.ProblemaDelEjercicio
import com.mastercode.app.data.problemas.problemaDesdeJson
import org.json.JSONException
import org.json.JSONObject

// Una sesión del Dojo recién abierta (HU53-T3): su id, el lenguaje y la dificultad con la que parte.
data class SesionIniciada(val id: Long, val lenguaje: String, val nivel: String)

// HU55-T2: cómo va una sesión según user-api (GET /api/dojo/sesiones/{id}). ejercicioActual es el que se entregó y se dejó
// sin responder; null si no hay ninguno (o ya no es visible): entonces se pide el siguiente.
data class EstadoDeSesionServidor(
    val id: Long,
    val lenguaje: String,
    val abierta: Boolean,
    val aciertos: Int,
    val respondidos: Int,
    val ejercicioActual: ProblemaDelEjercicio?,
)

// Un tema donde el estudiante falló en la sesión y cuántas veces.
data class TemaFallado(val tema: String, val fallos: Int)

// HU55-T3: lo que devuelve POST /api/dojo/sesiones/{id}/terminar. cinturonNivel y cinturonPuntos son null si en esa sesión
// no sumó puntos de cinturón.
data class ResumenDeSesion(
    val nivelAlcanzado: String,
    val respondidos: Int,
    val aciertos: Int,
    val puntosSumados: Int,
    val temasMasFallados: List<TemaFallado>,
    val cinturonNivel: Int?,
    val cinturonPuntos: Int?,
)

// Llamadas al Dojo de user-api. Lanzan ErrorHttp si el servidor rechaza y IOException si no hay conexión.
interface DojoApi {
    suspend fun iniciar(access: String, lenguaje: String): SesionIniciada

    // null cuando ya no quedan ejercicios (204).
    suspend fun siguiente(access: String, sesionId: Long): ProblemaDelEjercicio?

    // HU55-T2: no entrega ni apunta nada, solo lee. Lanza ErrorHttp(404) si la sesión no es del estudiante.
    suspend fun sesion(access: String, sesionId: Long): EstadoDeSesionServidor

    // HU55-T3: cierra la sesión. Lanza ErrorHttp(404) si no es del estudiante y ErrorHttp(409) si ya estaba cerrada.
    suspend fun terminar(access: String, sesionId: Long): ResumenDeSesion
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

    override suspend fun sesion(access: String, sesionId: Long): EstadoDeSesionServidor {
        val respuesta = pedirJson(urlBase + "api/dojo/sesiones/$sesionId", access)
        if (respuesta.codigo != 200) throw ErrorHttp(respuesta.codigo)
        return try {
            val json = JSONObject(respuesta.texto)
            EstadoDeSesionServidor(
                id = json.getLong("id"),
                lenguaje = json.getString("lenguaje"),
                abierta = json.getBoolean("abierta"),
                aciertos = json.getInt("aciertos"),
                respondidos = json.getInt("respondidos"),
                ejercicioActual = json.optJSONObject("ejercicio_actual")?.let { problemaDesdeJson(it) },
            )
        } catch (e: JSONException) {
            throw ErrorHttp(respuesta.codigo)
        }
    }

    override suspend fun terminar(access: String, sesionId: Long): ResumenDeSesion {
        val respuesta = pedirJson(urlBase + "api/dojo/sesiones/$sesionId/terminar", access, "POST")
        if (respuesta.codigo != 200) throw ErrorHttp(respuesta.codigo)
        return try {
            val json = JSONObject(respuesta.texto)
            val temas = json.optJSONArray("temas_mas_fallados")
            val cinturon = json.optJSONObject("cinturon")
            ResumenDeSesion(
                nivelAlcanzado = json.getString("nivel_alcanzado"),
                respondidos = json.getInt("respondidos"),
                aciertos = json.getInt("aciertos"),
                puntosSumados = json.getInt("puntos_sumados"),
                temasMasFallados = (0 until (temas?.length() ?: 0)).map { i ->
                    val tema = temas!!.getJSONObject(i)
                    TemaFallado(tema.getString("tema"), tema.getInt("fallos"))
                },
                cinturonNivel = cinturon?.getInt("nivel"),
                cinturonPuntos = cinturon?.getInt("puntos"),
            )
        } catch (e: JSONException) {
            throw ErrorHttp(respuesta.codigo)
        }
    }
}
