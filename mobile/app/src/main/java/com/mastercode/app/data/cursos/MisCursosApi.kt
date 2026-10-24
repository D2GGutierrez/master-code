package com.mastercode.app.data.cursos

import com.mastercode.app.data.auth.ErrorHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

// Un curso de GET /api/mis-cursos (HU27-T1). institucion también llega, pero la pantalla no la usa.
data class CursoInscrito(
    val cursoId: Long,
    val nombre: String,
    val lenguaje: String,
    val fechaInscripcion: LocalDate?,
)

// Llamadas a user-api. Lanza ErrorHttp si el servidor rechaza y IOException si no hay conexión.
interface MisCursosApi {
    suspend fun misCursos(access: String): List<CursoInscrito>
}

class HttpMisCursosApi(private val urlBase: String) : MisCursosApi {

    // Igual que HttpAuthApi: HttpURLConnection y org.json, que ya vienen en Android.
    override suspend fun misCursos(access: String): List<CursoInscrito> = withContext(Dispatchers.IO) {
        val conexion = URL(urlBase + "api/mis-cursos").openConnection() as HttpURLConnection
        try {
            conexion.connectTimeout = 10_000
            conexion.readTimeout = 10_000
            conexion.setRequestProperty("Authorization", "Bearer $access")

            val codigo = conexion.responseCode
            if (codigo != HttpURLConnection.HTTP_OK) throw ErrorHttp(codigo)
            val texto = conexion.inputStream.bufferedReader().use { it.readText() }
            try {
                val lista = JSONArray(texto)
                (0 until lista.length()).map { i ->
                    val curso = lista.getJSONObject(i)
                    CursoInscrito(
                        cursoId = curso.getLong("curso_id"),
                        nombre = curso.getString("nombre"),
                        lenguaje = curso.getString("lenguaje"),
                        fechaInscripcion = fecha(curso.optString("fecha_inscripcion")),
                    )
                }
            } catch (e: JSONException) {
                // Un 200 sin el formato esperado se trata como error del servidor.
                throw ErrorHttp(codigo)
            }
        } finally {
            conexion.disconnect()
        }
    }

    // La fecha es un dato secundario: si no llega o no se entiende, la tarjeta se muestra sin ella.
    private fun fecha(texto: String): LocalDate? = try {
        OffsetDateTime.parse(texto).toLocalDate()
    } catch (e: DateTimeParseException) {
        null
    }
}
