package com.mastercode.app.data.problemas

// API falsa: cada prueba decide qué responde (o qué lanza) user-api. Guarda los access y los ids recibidos.
class FakeProblemaApi : ProblemaApi {
    var respuesta: suspend (access: String, id: Long) -> ProblemaDelEjercicio = { _, id ->
        ProblemaDelEjercicio(id, "Ejercicio", "¿Qué imprime?", "prediccion_salida")
    }
    val accessRecibidos = mutableListOf<String>()
    val idsPedidos = mutableListOf<Long>()

    // Por defecto el servidor acepta todo y dice que acertó.
    var alResponder: suspend (access: String, respuesta: RespuestaDelEstudiante) -> ResultadoDeRespuesta = { _, _ ->
        ResultadoDeRespuesta(acerto = true, respuestaCorrecta = null, intentosRestantes = 2)
    }
    val respuestasRecibidas = mutableListOf<RespuestaDelEstudiante>()

    override suspend fun problema(access: String, id: Long): ProblemaDelEjercicio {
        accessRecibidos += access
        idsPedidos += id
        return respuesta(access, id)
    }

    override suspend fun responder(access: String, respuesta: RespuestaDelEstudiante): ResultadoDeRespuesta {
        respuestasRecibidas += respuesta
        return alResponder(access, respuesta)
    }
}
