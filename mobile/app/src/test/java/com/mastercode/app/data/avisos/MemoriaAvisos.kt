package com.mastercode.app.data.avisos

// Reemplaza a SharedPreferences, que no existe en la JVM de las pruebas.
class MemoriaAvisos(var ajustes: AjustesDeAvisos = AjustesDeAvisos()) : AlmacenDeAvisos {
    val guardados = mutableListOf<AjustesDeAvisos>()

    override fun leer(): AjustesDeAvisos = ajustes

    override fun guardar(ajustes: AjustesDeAvisos) {
        this.ajustes = ajustes
        guardados += ajustes
    }
}
