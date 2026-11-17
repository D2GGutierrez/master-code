package com.mastercode.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Lo que contestó user-api: el código y el cuerpo (vacío si no hay, como en un 204). No lanza por un código de error:
// cada llamada decide qué hacer con cada uno. Sí lanza IOException si no hay conexión.
internal class RespuestaHttp(val codigo: Int, val texto: String)

// HttpURLConnection, como el resto de las APIs. Con cuerpo, lo manda como JSON.
internal suspend fun pedirJson(
    url: String,
    access: String,
    metodo: String = "GET",
    cuerpo: JSONObject? = null,
): RespuestaHttp = withContext(Dispatchers.IO) {
    val conexion = URL(url).openConnection() as HttpURLConnection
    try {
        conexion.requestMethod = metodo
        conexion.connectTimeout = 10_000
        conexion.readTimeout = 10_000
        conexion.setRequestProperty("Authorization", "Bearer $access")
        if (cuerpo != null) {
            conexion.doOutput = true
            conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conexion.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }
        }
        val codigo = conexion.responseCode
        val flujo = if (codigo >= 400) conexion.errorStream else conexion.inputStream
        val texto = flujo?.bufferedReader()?.use { it.readText() } ?: ""
        RespuestaHttp(codigo, texto)
    } finally {
        conexion.disconnect()
    }
}
