package com.mastercode.app.data.avisos

// HU52-T3: los avisos de práctica que el estudiante quiere recibir y a qué hora. Se guardan solo en el teléfono (ver
// docs/notificaciones-push.md): por defecto están activados y llegan a las 20:00.
data class AjustesDeAvisos(
    val activados: Boolean = true,
    val hora: Int = HORA_POR_DEFECTO,
    val minuto: Int = MINUTO_POR_DEFECTO,
) {
    companion object {
        const val HORA_POR_DEFECTO = 20
        const val MINUTO_POR_DEFECTO = 0

        fun horaValida(hora: Int, minuto: Int) = hora in 0..23 && minuto in 0..59
    }
}

// Se reemplaza en las pruebas: SharedPreferences no existe en la JVM.
interface AlmacenDeAvisos {
    fun leer(): AjustesDeAvisos
    fun guardar(ajustes: AjustesDeAvisos)
}
