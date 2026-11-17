package com.mastercode.app.data.respuestas

import com.mastercode.app.data.problemas.RespuestaDelEstudiante

// Reemplazan a SharedPreferences, que no existe en la JVM de las pruebas.
class MemoriaPendientes : AlmacenDePendientes {
    var guardadas: List<RespuestaDelEstudiante> = emptyList()

    override fun leer(): List<RespuestaDelEstudiante> = guardadas

    override fun guardar(lista: List<RespuestaDelEstudiante>) {
        guardadas = lista
    }
}

class MemoriaSesion : AlmacenDeSesion {
    var estado: EstadoDeSesion? = null
    val guardados = mutableListOf<EstadoDeSesion?>()

    override fun leer(): EstadoDeSesion? = estado

    override fun guardar(estado: EstadoDeSesion?) {
        this.estado = estado
        guardados += estado
    }
}
