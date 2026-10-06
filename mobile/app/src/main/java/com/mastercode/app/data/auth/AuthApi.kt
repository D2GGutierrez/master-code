package com.mastercode.app.data.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Tokens(val access: String, val refresh: String)

// El servidor respondió con un código distinto de 200. El mensaje solo lleva el código, nunca tokens.
class ErrorHttp(val codigo: Int) : Exception("HTTP $codigo")

// Llamadas a admin-api. Lanza ErrorHttp si el servidor rechaza y IOException si no hay conexión.
interface AuthApi {
    suspend fun login(email: String, password: String): Tokens
    suspend fun renovar(refresh: String): Tokens
}

class HttpAuthApi(private val urlBase: String) : AuthApi {

    override suspend fun login(email: String, password: String): Tokens {
        // "mobile" hace que admin-api emita el refresh de 30 días.
        val cuerpo = JSONObject().put("email", email).put("password", password).put("cliente", "mobile")
        return post("api/auth/login/", cuerpo)
    }

    override suspend fun renovar(refresh: String): Tokens =
        post("api/auth/refresh/", JSONObject().put("refresh", refresh))

    // Se usa HttpURLConnection y org.json, que ya vienen en Android, para no sumar dependencias.
    private suspend fun post(ruta: String, cuerpo: JSONObject): Tokens = withContext(Dispatchers.IO) {
        val conexion = URL(urlBase + ruta).openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "POST"
            conexion.connectTimeout = 10_000
            conexion.readTimeout = 10_000
            conexion.doOutput = true
            conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conexion.outputStream.use { it.write(cuerpo.toString().toByteArray()) }

            val codigo = conexion.responseCode
            if (codigo != HttpURLConnection.HTTP_OK) throw ErrorHttp(codigo)
            val texto = conexion.inputStream.bufferedReader().use { it.readText() }
            try {
                val json = JSONObject(texto)
                Tokens(access = json.getString("access"), refresh = json.getString("refresh"))
            } catch (e: JSONException) {
                // Un 200 sin los tokens esperados se trata como error del servidor.
                throw ErrorHttp(codigo)
            }
        } finally {
            conexion.disconnect()
        }
    }
}
