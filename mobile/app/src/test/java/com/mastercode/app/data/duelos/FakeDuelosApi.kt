package com.mastercode.app.data.duelos

import com.mastercode.app.data.problemas.ProblemaDelEjercicio

// API falsa de los duelos: cada prueba decide qué responde (o qué lanza). Cada llamada queda anotada.
class FakeDuelosApi : DuelosApi {
    var alPedirCompaneros: suspend (cursoId: Long) -> List<Companero> = { emptyList() }
    var alRetar: suspend (cursoId: Long, rivalId: Long) -> RetoCreado = { cursoId, _ -> RetoCreado(11, cursoId, "beto", 3) }
    var alPedirInvitaciones: suspend () -> List<Invitacion> = { emptyList() }
    var alAceptar: suspend (partidaId: Long) -> Marcador = { marcadorDe(it) }
    var alRechazar: suspend (partidaId: Long) -> Unit = {}
    var alPedirEjercicio: suspend (partidaId: Long) -> EjercicioDelTurno? = { null }
    var alResponder: suspend (partidaId: Long, turno: Int, texto: String?, opcionId: Long?) -> RespuestaDePartida =
        { partidaId, _, _, _ -> RespuestaDePartida(true, null, marcadorDe(partidaId)) }
    // Lo que contesta el marcador; con el mismo etag que manda la app, contesta «no cambió» como el 304.
    var alPedirMarcador: suspend (partidaId: Long) -> LecturaDeMarcador = { LecturaDeMarcador(marcadorDe(it), "\"1\"") }
    var alPedirHistorial: suspend (pagina: Int, tamano: Int) -> PaginaDeHistorial = { pagina, tamano -> PaginaDeHistorial(emptyList(), pagina, tamano, 0) }
    var alPedirRanking: suspend (cursoId: Long, pagina: Int, tamano: Int) -> Ranking =
        { cursoId, pagina, tamano -> Ranking(cursoId, 0, pagina, tamano, emptyList(), null) }

    val companerosPedidos = mutableListOf<Long>()
    val retosEnviados = mutableListOf<Pair<Long, Long>>()
    val aceptadas = mutableListOf<Long>()
    val rechazadas = mutableListOf<Long>()
    val respuestasEnviadas = mutableListOf<RespuestaEnviada>()
    val etagsEnviados = mutableListOf<String?>()
    val historialesPedidos = mutableListOf<Pair<Int, Int>>()
    val rankingsPedidos = mutableListOf<Triple<Long, Int, Int>>()

    data class RespuestaEnviada(val partidaId: Long, val turno: Int, val texto: String?, val opcionId: Long?)

    override suspend fun companeros(access: String, cursoId: Long): List<Companero> {
        companerosPedidos += cursoId
        return alPedirCompaneros(cursoId)
    }

    override suspend fun retar(access: String, cursoId: Long, rivalId: Long): RetoCreado {
        retosEnviados += cursoId to rivalId
        return alRetar(cursoId, rivalId)
    }

    override suspend fun invitaciones(access: String): List<Invitacion> = alPedirInvitaciones()

    override suspend fun aceptar(access: String, partidaId: Long): Marcador {
        aceptadas += partidaId
        return alAceptar(partidaId)
    }

    override suspend fun rechazar(access: String, partidaId: Long) {
        rechazadas += partidaId
        alRechazar(partidaId)
    }

    override suspend fun ejercicio(access: String, partidaId: Long): EjercicioDelTurno? = alPedirEjercicio(partidaId)

    override suspend fun responder(access: String, partidaId: Long, turno: Int, texto: String?, opcionId: Long?): RespuestaDePartida {
        respuestasEnviadas += RespuestaEnviada(partidaId, turno, texto, opcionId)
        return alResponder(partidaId, turno, texto, opcionId)
    }

    override suspend fun marcador(access: String, partidaId: Long, etag: String?): LecturaDeMarcador {
        etagsEnviados += etag
        val lectura = alPedirMarcador(partidaId)
        return if (etag != null && etag == lectura.etag) LecturaDeMarcador(null, etag) else lectura
    }

    override suspend fun historial(access: String, pagina: Int, tamano: Int): PaginaDeHistorial {
        historialesPedidos += pagina to tamano
        return alPedirHistorial(pagina, tamano)
    }

    override suspend fun ranking(access: String, cursoId: Long, pagina: Int, tamano: Int): Ranking {
        rankingsPedidos += Triple(cursoId, pagina, tamano)
        return alPedirRanking(cursoId, pagina, tamano)
    }

    companion object {
        fun marcadorDe(
            partidaId: Long,
            estado: String = "en_curso",
            yo: Int = 0,
            rival: Int = 0,
            respondidosYo: Int = 0,
            respondidosRival: Int = 0,
            rivalConectado: Boolean = true,
            resultado: String? = null,
            motivoFin: String? = null,
            total: Int = 3,
        ) = Marcador(
            partidaId, 3, estado, total,
            JugadorDelMarcador("ana", yo, respondidosYo), JugadorDelMarcador("beto", rival, respondidosRival),
            rivalConectado, resultado, motivoFin,
        )

        fun ejercicioDe(turno: Int, total: Int = 3, tipo: String = "prediccion_salida") =
            EjercicioDelTurno(turno, total, ProblemaDelEjercicio(100L + turno, "Ejercicio $turno", "¿Qué imprime?", tipo))
    }
}
