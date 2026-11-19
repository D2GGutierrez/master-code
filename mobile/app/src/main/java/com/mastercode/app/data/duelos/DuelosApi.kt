package com.mastercode.app.data.duelos

import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.pedirJson
import com.mastercode.app.data.problemas.ProblemaDelEjercicio
import com.mastercode.app.data.problemas.problemaDesdeJson
import org.json.JSONException
import org.json.JSONObject

// Un compañero del curso al que se puede retar (HU57-T3). user-api manda el alias, nunca el correo.
data class Companero(val id: Long, val alias: String)

// El reto recién creado (HU57-T2): queda pendiente hasta que el rival lo acepte.
data class RetoCreado(val id: Long, val cursoId: Long, val rival: String, val totalEjercicios: Int)

// Un reto que otro estudiante le hizo a este (H7). venceEn es la fecha en que caduca, tal como la manda user-api.
data class Invitacion(val id: Long, val cursoId: Long, val retador: String, val venceEn: String?)

data class JugadorDelMarcador(val alias: String, val puntaje: Int, val respondidos: Int)

// HU59-T2: el marcador de una partida visto por quien lo pide. estado: pendiente, en_curso, terminada, rechazada o
// caducada. resultado (gane, perdi o empate) y motivoFin (completada o abandono) son null mientras no termina.
data class Marcador(
    val partidaId: Long,
    val cursoId: Long,
    val estado: String,
    val totalEjercicios: Int,
    val yo: JugadorDelMarcador,
    val rival: JugadorDelMarcador,
    val rivalConectado: Boolean,
    val resultado: String?,
    val motivoFin: String?,
) {
    val terminada get() = estado == "terminada" || estado == "rechazada" || estado == "caducada"
}

// Lo que devuelve consultar el marcador: el marcador nuevo y su ETag, o null si no cambió (304).
data class LecturaDeMarcador(val marcador: Marcador?, val etag: String?)

// El ejercicio del turno (HU58-T2): en qué turno va el estudiante y el ejercicio a resolver.
data class EjercicioDelTurno(val turno: Int, val totalEjercicios: Int, val problema: ProblemaDelEjercicio)

// HU58-T3: lo que contesta user-api a una respuesta. respuestaCorrecta solo viene si falló.
data class RespuestaDePartida(val acerto: Boolean, val respuestaCorrecta: String?, val marcador: Marcador)

// HU60-T1: una partida del historial. fecha es la de cierre (o la de inicio si no terminó).
data class PartidaDelHistorial(
    val id: Long,
    val cursoId: Long,
    val estado: String,
    val rival: String,
    val miPuntaje: Int,
    val puntajeRival: Int,
    val resultado: String?,
    val motivoFin: String?,
    val fecha: String?,
)

data class PaginaDeHistorial(val items: List<PartidaDelHistorial>, val pagina: Int, val tamano: Int, val total: Int)

// HU60-T2: una fila del ranking del curso.
data class PuestoDelRanking(
    val posicion: Int,
    val alias: String,
    val jugadas: Int,
    val ganadas: Int,
    val empatadas: Int,
    val perdidas: Int,
    val puntos: Int,
    val porcentajeVictorias: Double,
    val soyYo: Boolean,
)

// yo es el puesto del estudiante esté o no en la página; null si todavía no jugó en el curso.
data class Ranking(
    val cursoId: Long,
    val total: Int,
    val pagina: Int,
    val tamano: Int,
    val items: List<PuestoDelRanking>,
    val yo: PuestoDelRanking?,
)

// Llamadas a los duelos de user-api. Lanzan ErrorHttp si el servidor rechaza y IOException si no hay conexión.
interface DuelosApi {
    // 403 si el estudiante no está inscrito en el curso.
    suspend fun companeros(access: String, cursoId: Long): List<Companero>

    // 400 (rival inválido), 403 (no inscrito), 409 (ya hay un reto vivo o el curso no tiene ejercicios móviles), 503.
    suspend fun retar(access: String, cursoId: Long, rivalId: Long): RetoCreado

    suspend fun invitaciones(access: String): List<Invitacion>

    // 403 si el reto es propio, 404 si no es de la partida, 409 si caducó o ya se respondió.
    suspend fun aceptar(access: String, partidaId: Long): Marcador

    suspend fun rechazar(access: String, partidaId: Long)

    // null (204) si ya respondió todos y espera al rival. 409 si la partida no está en curso.
    suspend fun ejercicio(access: String, partidaId: Long): EjercicioDelTurno?

    // texto para los ejercicios de escribir, opcionId para los de opción múltiple. 409 si el turno ya no es ese.
    suspend fun responder(access: String, partidaId: Long, turno: Int, texto: String?, opcionId: Long?): RespuestaDePartida

    // etag: el de la última lectura; si el marcador no cambió, la lectura trae marcador null.
    suspend fun marcador(access: String, partidaId: Long, etag: String?): LecturaDeMarcador

    suspend fun historial(access: String, pagina: Int, tamano: Int): PaginaDeHistorial

    suspend fun ranking(access: String, cursoId: Long, pagina: Int, tamano: Int): Ranking
}

// Igual que las otras APIs: HttpURLConnection y org.json, que ya vienen en Android. Un 200 con otro formato se trata como
// error del servidor.
class HttpDuelosApi(private val urlBase: String) : DuelosApi {

    override suspend fun companeros(access: String, cursoId: Long): List<Companero> {
        val respuesta = pedirJson(urlBase + "api/cursos/$cursoId/companeros", access)
        if (respuesta.codigo != 200) throw ErrorHttp(respuesta.codigo)
        return leer(respuesta.codigo) {
            val lista = org.json.JSONArray(respuesta.texto)
            (0 until lista.length()).map { i -> lista.getJSONObject(i).let { Companero(it.getLong("id"), it.getString("alias")) } }
        }
    }

    override suspend fun retar(access: String, cursoId: Long, rivalId: Long): RetoCreado {
        val cuerpo = JSONObject().put("curso_id", cursoId).put("rival_id", rivalId)
        val respuesta = pedirJson(urlBase + "api/partidas", access, "POST", cuerpo)
        if (respuesta.codigo != 201) throw ErrorHttp(respuesta.codigo)
        return leer(respuesta.codigo) {
            val json = JSONObject(respuesta.texto)
            RetoCreado(json.getLong("id"), json.getLong("curso_id"), json.getString("rival"), json.getInt("total_ejercicios"))
        }
    }

    override suspend fun invitaciones(access: String): List<Invitacion> {
        val respuesta = pedirJson(urlBase + "api/partidas/invitaciones", access)
        if (respuesta.codigo != 200) throw ErrorHttp(respuesta.codigo)
        return leer(respuesta.codigo) {
            val lista = org.json.JSONArray(respuesta.texto)
            (0 until lista.length()).map { i ->
                val json = lista.getJSONObject(i)
                Invitacion(json.getLong("id"), json.getLong("curso_id"), json.getString("retador"), textoONulo(json, "vence_en"))
            }
        }
    }

    override suspend fun aceptar(access: String, partidaId: Long): Marcador {
        val respuesta = pedirJson(urlBase + "api/partidas/$partidaId/aceptar", access, "POST")
        if (respuesta.codigo != 200) throw ErrorHttp(respuesta.codigo)
        return leer(respuesta.codigo) { marcadorDesdeJson(JSONObject(respuesta.texto)) }
    }

    override suspend fun rechazar(access: String, partidaId: Long) {
        val respuesta = pedirJson(urlBase + "api/partidas/$partidaId/rechazar", access, "POST")
        if (respuesta.codigo != 204) throw ErrorHttp(respuesta.codigo)
    }

    override suspend fun ejercicio(access: String, partidaId: Long): EjercicioDelTurno? {
        val respuesta = pedirJson(urlBase + "api/partidas/$partidaId/ejercicio", access)
        if (respuesta.codigo == 204) return null
        if (respuesta.codigo != 200) throw ErrorHttp(respuesta.codigo)
        return leer(respuesta.codigo) {
            val json = JSONObject(respuesta.texto)
            EjercicioDelTurno(json.getInt("turno"), json.getInt("total_ejercicios"), problemaDesdeJson(json.getJSONObject("ejercicio")))
        }
    }

    override suspend fun responder(access: String, partidaId: Long, turno: Int, texto: String?, opcionId: Long?): RespuestaDePartida {
        val cuerpo = JSONObject().put("turno", turno)
        if (texto != null) cuerpo.put("respuesta", texto)
        if (opcionId != null) cuerpo.put("opcion_id", opcionId)
        val respuesta = pedirJson(urlBase + "api/partidas/$partidaId/respuesta", access, "POST", cuerpo)
        if (respuesta.codigo != 200) throw ErrorHttp(respuesta.codigo)
        return leer(respuesta.codigo) {
            val json = JSONObject(respuesta.texto)
            RespuestaDePartida(
                acerto = json.getString("resultado") == "correcto",
                respuestaCorrecta = textoONulo(json, "respuesta_correcta"),
                marcador = marcadorDesdeJson(json.getJSONObject("marcador")),
            )
        }
    }

    override suspend fun marcador(access: String, partidaId: Long, etag: String?): LecturaDeMarcador {
        val cabeceras = if (etag != null) mapOf("If-None-Match" to etag) else emptyMap()
        val respuesta = pedirJson(urlBase + "api/partidas/$partidaId/marcador", access, cabeceras = cabeceras)
        if (respuesta.codigo == 304) return LecturaDeMarcador(null, etag)
        if (respuesta.codigo != 200) throw ErrorHttp(respuesta.codigo)
        return leer(respuesta.codigo) { LecturaDeMarcador(marcadorDesdeJson(JSONObject(respuesta.texto)), respuesta.etag) }
    }

    override suspend fun historial(access: String, pagina: Int, tamano: Int): PaginaDeHistorial {
        val respuesta = pedirJson(urlBase + "api/partidas?pagina=$pagina&tamano=$tamano", access)
        if (respuesta.codigo != 200) throw ErrorHttp(respuesta.codigo)
        return leer(respuesta.codigo) {
            val json = JSONObject(respuesta.texto)
            val lista = json.getJSONArray("items")
            PaginaDeHistorial(
                items = (0 until lista.length()).map { i ->
                    val fila = lista.getJSONObject(i)
                    PartidaDelHistorial(
                        id = fila.getLong("id"),
                        cursoId = fila.getLong("curso_id"),
                        estado = fila.getString("estado"),
                        rival = fila.getString("rival"),
                        miPuntaje = fila.getInt("mi_puntaje"),
                        puntajeRival = fila.getInt("puntaje_rival"),
                        resultado = textoONulo(fila, "resultado"),
                        motivoFin = textoONulo(fila, "motivo_fin"),
                        fecha = textoONulo(fila, "fecha_fin") ?: textoONulo(fila, "fecha_inicio"),
                    )
                },
                pagina = json.getInt("pagina"),
                tamano = json.getInt("tamano"),
                total = json.getInt("total"),
            )
        }
    }

    override suspend fun ranking(access: String, cursoId: Long, pagina: Int, tamano: Int): Ranking {
        val respuesta = pedirJson(urlBase + "api/cursos/$cursoId/ranking?pagina=$pagina&tamano=$tamano", access)
        if (respuesta.codigo != 200) throw ErrorHttp(respuesta.codigo)
        return leer(respuesta.codigo) {
            val json = JSONObject(respuesta.texto)
            val lista = json.getJSONArray("items")
            Ranking(
                cursoId = json.getLong("curso_id"),
                total = json.getInt("total"),
                pagina = json.getInt("pagina"),
                tamano = json.getInt("tamano"),
                items = (0 until lista.length()).map { i -> puestoDesdeJson(lista.getJSONObject(i)) },
                yo = json.optJSONObject("yo")?.let { puestoDesdeJson(it) },
            )
        }
    }

    // Los campos que user-api manda como null (resultado, motivo_fin, fechas) o no manda: org.json devolvería "null".
    private fun textoONulo(json: JSONObject, campo: String): String? =
        if (json.isNull(campo)) null else json.getString(campo).ifEmpty { null }

    // Un 200 con un formato que no es el esperado se trata como error del servidor.
    private fun <T> leer(codigo: Int, interpretar: () -> T): T = try {
        interpretar()
    } catch (e: JSONException) {
        throw ErrorHttp(codigo)
    }

    private fun jugadorDesdeJson(json: JSONObject) =
        JugadorDelMarcador(json.getString("alias"), json.getInt("puntaje"), json.getInt("respondidos"))

    private fun marcadorDesdeJson(json: JSONObject) = Marcador(
        partidaId = json.getLong("partida_id"),
        cursoId = json.getLong("curso_id"),
        estado = json.getString("estado"),
        totalEjercicios = json.getInt("total_ejercicios"),
        yo = jugadorDesdeJson(json.getJSONObject("yo")),
        rival = jugadorDesdeJson(json.getJSONObject("rival")),
        rivalConectado = json.getBoolean("rival_conectado"),
        resultado = textoONulo(json, "resultado"),
        motivoFin = textoONulo(json, "motivo_fin"),
    )

    private fun puestoDesdeJson(json: JSONObject) = PuestoDelRanking(
        posicion = json.getInt("posicion"),
        alias = json.getString("alias"),
        jugadas = json.getInt("jugadas"),
        ganadas = json.getInt("ganadas"),
        empatadas = json.getInt("empatadas"),
        perdidas = json.getInt("perdidas"),
        puntos = json.getInt("puntos"),
        porcentajeVictorias = json.getDouble("porcentaje_victorias"),
        soyYo = json.getBoolean("soy_yo"),
    )
}
