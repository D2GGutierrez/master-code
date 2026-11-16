package com.mastercode.app.data.problemas

// API falsa: cada prueba decide qué responde (o qué lanza) user-api. Guarda los access y los ids recibidos.
class FakeProblemaApi : ProblemaApi {
    var respuesta: suspend (access: String, id: Long) -> ProblemaDelEjercicio = { _, id ->
        ProblemaDelEjercicio(id, "Ejercicio", "¿Qué imprime?", "prediccion_salida")
    }
    val accessRecibidos = mutableListOf<String>()
    val idsPedidos = mutableListOf<Long>()

    override suspend fun problema(access: String, id: Long): ProblemaDelEjercicio {
        accessRecibidos += access
        idsPedidos += id
        return respuesta(access, id)
    }
}
