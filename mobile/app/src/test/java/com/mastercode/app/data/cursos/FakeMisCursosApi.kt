package com.mastercode.app.data.cursos

// API falsa: cada prueba decide qué responde (o qué lanza) user-api. Guarda los access recibidos.
class FakeMisCursosApi : MisCursosApi {
    var respuesta: suspend (access: String) -> List<CursoInscrito> = { emptyList() }
    val accessRecibidos = mutableListOf<String>()

    override suspend fun misCursos(access: String): List<CursoInscrito> {
        accessRecibidos += access
        return respuesta(access)
    }
}
