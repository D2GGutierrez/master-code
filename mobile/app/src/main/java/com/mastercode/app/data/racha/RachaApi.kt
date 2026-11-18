package com.mastercode.app.data.racha

import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.pedirJson
import org.json.JSONException
import org.json.JSONObject

// La racha de un lenguaje: los días seguidos de ahora y la mejor que ha tenido.
data class RachaDeLenguaje(val lenguaje: String, val rachaActual: Int, val rachaMaxima: Int, val practicoHoy: Boolean)

// HU49-T3: la racha del estudiante según GET /api/yo/racha. rachaActual es la más alta entre los lenguajes y practicoHoy es
// verdadero si practicó hoy en cualquiera (ADR-13, D23): es lo que muestra la pantalla principal.
data class Racha(val rachaActual: Int, val practicoHoy: Boolean, val porLenguaje: List<RachaDeLenguaje>)

// Lanza ErrorHttp si el servidor rechaza y IOException si no hay conexión.
interface RachaApi {
    suspend fun racha(access: String): Racha
}

class HttpRachaApi(private val urlBase: String) : RachaApi {

    override suspend fun racha(access: String): Racha {
        val respuesta = pedirJson(urlBase + "api/yo/racha", access)
        if (respuesta.codigo != 200) throw ErrorHttp(respuesta.codigo)
        return try {
            val json = JSONObject(respuesta.texto)
            val lenguajes = json.getJSONArray("por_lenguaje")
            Racha(
                rachaActual = json.getInt("racha_actual"),
                practicoHoy = json.getBoolean("practico_hoy"),
                porLenguaje = (0 until lenguajes.length()).map { i ->
                    val l = lenguajes.getJSONObject(i)
                    RachaDeLenguaje(l.getString("lenguaje"), l.getInt("racha_actual"), l.getInt("racha_maxima"), l.getBoolean("practico_hoy"))
                },
            )
        } catch (e: JSONException) {
            throw ErrorHttp(respuesta.codigo)
        }
    }
}
