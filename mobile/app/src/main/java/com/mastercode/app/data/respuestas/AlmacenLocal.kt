package com.mastercode.app.data.respuestas

import com.mastercode.app.data.problemas.RespuestaDelEstudiante
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

// Lo que el teléfono guarda por su cuenta, sin librerías nuevas: JSON dentro de SharedPreferences (ver
// PreferenciasDelCelular). Aquí está lo que se puede probar en la JVM: las interfaces y el paso a JSON.

// Las respuestas que no pudieron enviarse por falta de señal (HU48-T3), en el orden en que se dieron.
interface AlmacenDePendientes {
    fun leer(): List<RespuestaDelEstudiante>
    fun guardar(lista: List<RespuestaDelEstudiante>)
}

// El estado de una sesión del Dojo (HU55-T1): cuál es, qué ejercicio se estaba haciendo y cuántos aciertos lleva.
data class EstadoDeSesion(
    val sesionId: Long,
    val lenguaje: String,
    val ejercicioActualId: Long?,
    val aciertos: Int,
    val respondidos: Int,
)

// Una sola sesión guardada: la última. null cuando no hay ninguna.
interface AlmacenDeSesion {
    fun leer(): EstadoDeSesion?
    fun guardar(estado: EstadoDeSesion?)
}

fun List<RespuestaDelEstudiante>.aJson(): String {
    val lista = JSONArray()
    for (r in this) {
        val json = JSONObject().put("problema_id", r.problemaId).put("intento_id", r.intentoId)
        r.texto?.let { json.put("texto", it) }
        r.opcionId?.let { json.put("opcion_id", it) }
        r.sesionId?.let { json.put("sesion_id", it) }
        lista.put(json)
    }
    return lista.toString()
}

// Un texto vacío, ausente o dañado da una lista vacía: no vale la pena trabar la app por un guardado ilegible.
fun respuestasDesdeJson(texto: String?): List<RespuestaDelEstudiante> {
    if (texto.isNullOrBlank()) return emptyList()
    return try {
        val lista = JSONArray(texto)
        (0 until lista.length()).map { i ->
            val json = lista.getJSONObject(i)
            RespuestaDelEstudiante(
                problemaId = json.getLong("problema_id"),
                texto = if (json.has("texto")) json.getString("texto") else null,
                opcionId = if (json.has("opcion_id")) json.getLong("opcion_id") else null,
                sesionId = if (json.has("sesion_id")) json.getLong("sesion_id") else null,
                intentoId = json.getString("intento_id"),
            )
        }
    } catch (e: JSONException) {
        emptyList()
    }
}

fun EstadoDeSesion.aJson(): String {
    val json = JSONObject()
        .put("sesion_id", sesionId)
        .put("lenguaje", lenguaje)
        .put("aciertos", aciertos)
        .put("respondidos", respondidos)
    ejercicioActualId?.let { json.put("ejercicio_actual_id", it) }
    return json.toString()
}

fun estadoDeSesionDesdeJson(texto: String?): EstadoDeSesion? {
    if (texto.isNullOrBlank()) return null
    return try {
        val json = JSONObject(texto)
        EstadoDeSesion(
            sesionId = json.getLong("sesion_id"),
            lenguaje = json.getString("lenguaje"),
            ejercicioActualId = if (json.has("ejercicio_actual_id")) json.getLong("ejercicio_actual_id") else null,
            aciertos = json.getInt("aciertos"),
            respondidos = json.getInt("respondidos"),
        )
    } catch (e: JSONException) {
        null
    }
}
