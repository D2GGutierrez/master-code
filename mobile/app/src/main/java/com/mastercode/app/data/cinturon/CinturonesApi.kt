package com.mastercode.app.data.cinturon

import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.pedirJson
import org.json.JSONException
import org.json.JSONObject

// El cinturón que viene después: su nivel, su nombre y los puntos acumulados desde los que se alcanza.
data class SiguienteCinturon(val nivel: Int, val nombre: String, val puntosRequeridos: Int)

// HU51-T2: el cinturón de un lenguaje según GET /api/yo/cinturones. Con el negro (el último), siguiente y puntosFaltantes
// son null. puntos es el total acumulado en ese lenguaje.
data class CinturonDelLenguaje(
    val lenguaje: String,
    val nivel: Int,
    val nombre: String,
    val puntos: Int,
    val siguiente: SiguienteCinturon?,
    val puntosFaltantes: Int?,
) {
    // HU51-T3: el negro no tiene siguiente: en vez de la barra se muestra un mensaje especial.
    val esElMaximo: Boolean get() = siguiente == null

    // Cuánto de la barra se llena, de 0 a 1: los puntos que lleva sobre los que pide el siguiente cinturón. null en el negro.
    val avance: Float?
        get() = siguiente?.let { if (it.puntosRequeridos <= 0) 1f else (puntos.toFloat() / it.puntosRequeridos).coerceIn(0f, 1f) }
}

// Lanza ErrorHttp si el servidor rechaza y IOException si no hay conexión.
interface CinturonesApi {
    // Un cinturón por cada lenguaje en que el estudiante ya acertó; sin ninguno, la lista va vacía.
    suspend fun cinturones(access: String): List<CinturonDelLenguaje>
}

class HttpCinturonesApi(private val urlBase: String) : CinturonesApi {

    override suspend fun cinturones(access: String): List<CinturonDelLenguaje> {
        val respuesta = pedirJson(urlBase + "api/yo/cinturones", access)
        if (respuesta.codigo != 200) throw ErrorHttp(respuesta.codigo)
        return try {
            val lista = JSONObject(respuesta.texto).getJSONArray("cinturones")
            (0 until lista.length()).map { i ->
                val c = lista.getJSONObject(i)
                // Las claves siguen en la respuesta del negro, pero con null.
                val siguiente = c.optJSONObject("siguiente")?.let {
                    SiguienteCinturon(it.getInt("nivel"), it.getString("nombre"), it.getInt("puntos_requeridos"))
                }
                CinturonDelLenguaje(
                    lenguaje = c.getString("lenguaje"),
                    nivel = c.getInt("nivel"),
                    nombre = c.getString("nombre"),
                    puntos = c.getInt("puntos"),
                    siguiente = siguiente,
                    puntosFaltantes = if (c.isNull("puntos_faltantes")) null else c.getInt("puntos_faltantes"),
                )
            }
        } catch (e: JSONException) {
            throw ErrorHttp(respuesta.codigo)
        }
    }
}
