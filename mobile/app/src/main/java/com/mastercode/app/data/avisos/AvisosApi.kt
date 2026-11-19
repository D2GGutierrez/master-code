package com.mastercode.app.data.avisos

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.conAccess
import com.mastercode.app.data.pedirJson
import org.json.JSONObject
import java.io.IOException

// HU52-T4: le dice a user-api si el estudiante quiere los avisos de práctica y a qué hora, para que el servidor mande el
// push a la hora elegida (PUT /api/yo/avisos). Lanza ErrorHttp si el servidor rechaza (400 si la hora no vale) y
// IOException si no hay conexión.
interface AvisosApi {
    suspend fun guardar(access: String, activados: Boolean, hora: String)
}

class HttpAvisosApi(private val urlBase: String) : AvisosApi {

    override suspend fun guardar(access: String, activados: Boolean, hora: String) {
        val cuerpo = JSONObject().put("activados", activados).put("hora", hora)
        val respuesta = pedirJson(urlBase + "api/yo/avisos", access, "PUT", cuerpo)
        // user-api contesta 204 sin cuerpo.
        if (respuesta.codigo != 204) throw ErrorHttp(respuesta.codigo)
    }
}

sealed interface ResultadoEnvioDeAvisos {
    data object Enviado : ResultadoEnvioDeAvisos
    data object SesionVencida : ResultadoEnvioDeAvisos
    data object SinConexion : ResultadoEnvioDeAvisos
    data object ErrorInesperado : ResultadoEnvioDeAvisos
}

// Manda los ajustes del teléfono a user-api. PUT es idempotente: se manda al cambiar un ajuste y otra vez al abrir la app con
// la sesión iniciada, así lo que no llegó por falta de señal llega después. Un fallo no molesta al estudiante.
class AvisosRepository(private val api: AvisosApi, private val auth: AuthRepository) {

    suspend fun enviar(ajustes: AjustesDeAvisos): ResultadoEnvioDeAvisos = auth.conAccess(
        vencida = ResultadoEnvioDeAvisos.SesionVencida,
        sinConexion = ResultadoEnvioDeAvisos.SinConexion,
        inesperado = ResultadoEnvioDeAvisos.ErrorInesperado,
    ) { access ->
        try {
            api.guardar(access, ajustes.activados, ajustes.horaComoTexto())
            ResultadoEnvioDeAvisos.Enviado
        } catch (e: ErrorHttp) {
            if (e.codigo == 401) null else ResultadoEnvioDeAvisos.ErrorInesperado
        } catch (e: IOException) {
            ResultadoEnvioDeAvisos.SinConexion
        }
    }
}
