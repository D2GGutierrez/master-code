package com.mastercode.app.data.perfil

import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.pedirJson
import org.json.JSONObject

// Llamada a user-api (H6): le dice en qué zona horaria está el teléfono, para que la racha cuente los días con la hora
// del estudiante y no con la del servidor. Lanza ErrorHttp si el servidor rechaza (400 si no es un nombre de región) y
// IOException si no hay conexión.
interface ZonaHorariaApi {
    suspend fun guardar(access: String, zonaHoraria: String)
}

// Igual que las otras APIs: HttpURLConnection y org.json, que ya vienen en Android.
class HttpZonaHorariaApi(private val urlBase: String) : ZonaHorariaApi {

    override suspend fun guardar(access: String, zonaHoraria: String) {
        val cuerpo = JSONObject().put("zona_horaria", zonaHoraria)
        val respuesta = pedirJson(urlBase + "api/yo/zona-horaria", access, "PUT", cuerpo)
        // user-api contesta 204 sin cuerpo.
        if (respuesta.codigo != 204) throw ErrorHttp(respuesta.codigo)
    }
}
