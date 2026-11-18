package com.mastercode.app.data.dispositivo

import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.pedirJson
import org.json.JSONObject

// HU52-T1 (ADR-9, D16): le dice a user-api cuál es el token de Firebase Cloud Messaging de este teléfono, para que pueda
// mandarle los avisos de práctica. Lanza ErrorHttp si el servidor rechaza (400 si el token viene vacío o es demasiado
// largo) y IOException si no hay conexión.
interface DispositivoApi {
    suspend fun registrar(access: String, token: String)
}

// Igual que las otras APIs: HttpURLConnection y org.json, que ya vienen en Android.
class HttpDispositivoApi(private val urlBase: String) : DispositivoApi {

    override suspend fun registrar(access: String, token: String) {
        val respuesta = pedirJson(urlBase + "api/dispositivo", access, "PUT", JSONObject().put("token", token))
        // user-api contesta 204 sin cuerpo.
        if (respuesta.codigo != 204) throw ErrorHttp(respuesta.codigo)
    }
}
