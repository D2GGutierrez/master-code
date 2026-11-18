package com.mastercode.app.data.racha

// API falsa: cada prueba decide qué responde (o qué lanza) user-api. Guarda los access recibidos.
class FakeRachaApi : RachaApi {
    var respuesta: suspend (access: String) -> Racha = { Racha(0, false, emptyList()) }
    val accessRecibidos = mutableListOf<String>()

    override suspend fun racha(access: String): Racha {
        accessRecibidos += access
        return respuesta(access)
    }
}
