package com.mastercode.app.data.problemas

import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.pedirJson
import org.json.JSONException
import org.json.JSONObject

// Una alternativa de un ejercicio de opción múltiple (HU46-T2). Solo id y texto: user-api no manda cuál es la correcta
// antes de responder (CA-46.4).
data class OpcionDelEjercicio(val id: Long, val texto: String)

// Un ejercicio de GET /api/problemas/{id} o de la entrega del Dojo (HU45-T2, HU47-T3, HU46-T3). Solo lo que las
// pantallas muestran: la respuesta esperada nunca viaja al celular, se corrige en user-api. Para predecir la salida y
// completar el espacio, el fragmento de código va dentro del enunciado (ADR-9, D17).
// intentosRestantes: los intentos que le quedan al estudiante en este ejercicio (HU45-T3); null si user-api no lo manda.
data class ProblemaDelEjercicio(
    val id: Long,
    val titulo: String,
    val enunciado: String,
    val tipoEjercicio: String,
    val opciones: List<OpcionDelEjercicio> = emptyList(),
    val intentosRestantes: Int? = null,
)

// Lo que el estudiante contesta (HU45-T3, HU46-T3, HU48-T3): lo escrito o la alternativa elegida. sesionId solo va si se
// responde dentro del Dojo. intentoId es único por intento: si el envío se repite porque se perdió la señal, user-api lo
// reconoce y no lo cuenta dos veces.
data class RespuestaDelEstudiante(
    val problemaId: Long,
    val texto: String?,
    val opcionId: Long?,
    val sesionId: Long?,
    val intentoId: String,
)

// Lo que user-api contesta a una respuesta (HU48-T2): si acertó y, solo si falló, cuál era la correcta.
data class ResultadoDeRespuesta(
    val acerto: Boolean,
    val respuestaCorrecta: String?,
    val intentosRestantes: Int,
)

// Llamadas a user-api. Lanza ErrorHttp si el servidor rechaza (404 si no existe o no es del estudiante; en responder,
// además 400 si la respuesta no vale y 409 si ya no se puede responder) y IOException si no hay conexión.
interface ProblemaApi {
    suspend fun problema(access: String, id: Long): ProblemaDelEjercicio

    suspend fun responder(access: String, respuesta: RespuestaDelEstudiante): ResultadoDeRespuesta
}

// Lee un ejercicio del JSON de user-api (el mismo formato en el detalle del problema y en la entrega del Dojo).
// Lanza JSONException si falta algo de lo que se muestra.
internal fun problemaDesdeJson(json: JSONObject): ProblemaDelEjercicio {
    val alternativas = json.optJSONArray("opciones")
    val opciones = (0 until (alternativas?.length() ?: 0)).map { i ->
        val opcion = alternativas!!.getJSONObject(i)
        OpcionDelEjercicio(opcion.getLong("id"), opcion.getString("texto"))
    }
    return ProblemaDelEjercicio(
        id = json.getLong("id"),
        titulo = json.getString("titulo"),
        enunciado = json.getString("enunciado"),
        tipoEjercicio = json.getString("tipo_ejercicio"),
        opciones = opciones,
        intentosRestantes = if (json.has("intentos_restantes")) json.getInt("intentos_restantes") else null,
    )
}

class HttpProblemaApi(private val urlBase: String) : ProblemaApi {

    // Igual que HttpMisCursosApi: HttpURLConnection y org.json, que ya vienen en Android.
    override suspend fun problema(access: String, id: Long): ProblemaDelEjercicio {
        val respuesta = pedirJson(urlBase + "api/problemas/$id", access)
        if (respuesta.codigo != 200) throw ErrorHttp(respuesta.codigo)
        return try {
            problemaDesdeJson(JSONObject(respuesta.texto))
        } catch (e: JSONException) {
            // Un 200 sin el formato esperado se trata como error del servidor.
            throw ErrorHttp(respuesta.codigo)
        }
    }

    override suspend fun responder(access: String, respuesta: RespuestaDelEstudiante): ResultadoDeRespuesta {
        val cuerpo = JSONObject().put("intento_id", respuesta.intentoId)
        respuesta.texto?.let { cuerpo.put("respuesta", it) }
        respuesta.opcionId?.let { cuerpo.put("opcion_id", it) }
        respuesta.sesionId?.let { cuerpo.put("sesion_id", it) }

        val recibida = pedirJson(urlBase + "api/problemas/${respuesta.problemaId}/respuesta", access, "POST", cuerpo)
        if (recibida.codigo != 200) throw ErrorHttp(recibida.codigo)
        return try {
            val json = JSONObject(recibida.texto)
            ResultadoDeRespuesta(
                acerto = json.getString("resultado") == "correcto",
                // La clave no viene cuando acertó.
                respuestaCorrecta = if (json.has("respuesta_correcta")) json.getString("respuesta_correcta") else null,
                intentosRestantes = json.getInt("intentos_restantes"),
            )
        } catch (e: JSONException) {
            throw ErrorHttp(recibida.codigo)
        }
    }
}
