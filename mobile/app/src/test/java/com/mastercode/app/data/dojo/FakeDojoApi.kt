package com.mastercode.app.data.dojo

import com.mastercode.app.data.problemas.ProblemaDelEjercicio

// API falsa del Dojo: cada prueba decide qué responde. siguiente entrega los ejercicios de la lista, uno tras otro, y
// null (204) cuando se acaban.
class FakeDojoApi : DojoApi {
    var alIniciar: suspend (lenguaje: String) -> SesionIniciada = { SesionIniciada(5, it, "facil") }
    var ejercicios: MutableList<ProblemaDelEjercicio> = mutableListOf()
    var alPedirSiguiente: (suspend () -> Unit)? = null
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
}
