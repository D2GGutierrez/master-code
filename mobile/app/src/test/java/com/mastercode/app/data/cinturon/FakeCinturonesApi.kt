package com.mastercode.app.data.cinturon

// API falsa: cada prueba decide qué responde (o qué lanza) user-api. Guarda los access recibidos.
class FakeCinturonesApi : CinturonesApi {
    var respuesta: suspend (access: String) -> List<CinturonDelLenguaje> = { emptyList() }
    val accessRecibidos = mutableListOf<String>()

    override suspend fun cinturones(access: String): List<CinturonDelLenguaje> {
        accessRecibidos += access
        return respuesta(access)
    }
}
