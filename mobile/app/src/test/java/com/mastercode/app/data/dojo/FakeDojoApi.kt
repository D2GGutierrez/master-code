package com.mastercode.app.data.dojo

import com.mastercode.app.data.problemas.ProblemaDelEjercicio

// API falsa del Dojo: cada prueba decide qué responde. siguiente entrega los ejercicios de la lista, uno tras otro, y
// null (204) cuando se acaban.
class FakeDojoApi : DojoApi {
    var alIniciar: suspend (lenguaje: String) -> SesionIniciada = { SesionIniciada(5, it, "facil") }
    var ejercicios: MutableList<ProblemaDelEjercicio> = mutableListOf()
    var alPedirSiguiente: (suspend () -> Unit)? = null
    // HU55-T2 y HU55-T3: lo que contesta user-api al consultar y al terminar la sesión.
    var alConsultar: suspend (sesionId: Long) -> EstadoDeSesionServidor = { EstadoDeSesionServidor(it, "python", true, 0, 0, null) }
    var alTerminar: suspend (sesionId: Long) -> ResumenDeSesion = { ResumenDeSesion("facil", 0, 0, 0, emptyList(), null, null) }
    val sesionesConsultadas = mutableListOf<Long>()
    val sesionesTerminadas = mutableListOf<Long>()
    val lenguajesPedidos = mutableListOf<String>()
    val sesionesPedidas = mutableListOf<Long>()

    override suspend fun iniciar(access: String, lenguaje: String): SesionIniciada {
        lenguajesPedidos += lenguaje
        return alIniciar(lenguaje)
    }

    override suspend fun siguiente(access: String, sesionId: Long): ProblemaDelEjercicio? {
        sesionesPedidas += sesionId
        alPedirSiguiente?.invoke()
        return if (ejercicios.isEmpty()) null else ejercicios.removeAt(0)
    }

    override suspend fun sesion(access: String, sesionId: Long): EstadoDeSesionServidor {
        sesionesConsultadas += sesionId
        return alConsultar(sesionId)
    }

    override suspend fun terminar(access: String, sesionId: Long): ResumenDeSesion {
        sesionesTerminadas += sesionId
        return alTerminar(sesionId)
    }
}
