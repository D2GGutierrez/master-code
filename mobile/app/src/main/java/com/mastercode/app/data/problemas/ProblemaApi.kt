package com.mastercode.app.data.problemas

import com.mastercode.app.data.auth.ErrorHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Un ejercicio de GET /api/problemas/{id} (HU45-T2, HU47-T3). Solo lo que las pantallas muestran: la respuesta esperada
// nunca viaja al celular, se corrige en user-api. Para predecir la salida y completar el espacio, el fragmento de código
// va dentro del enunciado (ADR-9, D17).
data class ProblemaDelEjercicio(
    val id: Long,
    val titulo: String,
    val enunciado: String,
    val tipoEjercicio: String,
)

// Llamadas a user-api. Lanza ErrorHttp si el servidor rechaza (404 si no existe o no es del estudiante) y IOException si
// no hay conexión.
interface ProblemaApi {
    suspend fun problema(access: String, id: Long): ProblemaDelEjercicio
}

class HttpProblemaApi(private val urlBase: String) : ProblemaApi {

    // Igual que HttpMisCursosApi: HttpURLConnection y org.json, que ya vienen en Android.
    override suspend fun problema(access: String, id: Long): ProblemaDelEjercicio = withContext(Dispatchers.IO) {
        val conexion = URL(urlBase + "api/problemas/$id").openConnection() as HttpURLConnection
        try {
            conexion.connectTimeout = 10_000
            conexion.readTimeout = 10_000
            conexion.setRequestProperty("Authorization", "Bearer $access")

            val codigo = conexion.responseCode
            if (codigo != HttpURLConnection.HTTP_OK) throw ErrorHttp(codigo)
            val texto = conexion.inputStream.bufferedReader().use { it.readText() }
            try {
                val problema = JSONObject(texto)
                ProblemaDelEjercicio(
                    id = problema.getLong("id"),
                    titulo = problema.getString("titulo"),
                    enunciado = problema.getString("enunciado"),
                    tipoEjercicio = problema.getString("tipo_ejercicio"),
                )
            } catch (e: JSONException) {
                // Un 200 sin el formato esperado se trata como error del servidor.
                throw ErrorHttp(codigo)
            }
        } finally {
            conexion.disconnect()
        }
    }
}
