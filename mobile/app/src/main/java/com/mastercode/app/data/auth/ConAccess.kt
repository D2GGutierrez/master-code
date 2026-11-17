package com.mastercode.app.data.auth

// La renovación del access que repiten las llamadas a user-api: se pide con el access en memoria; si falta o venció
// (pedir devuelve null, que significa 401), se renueva con el refresh y se pide una sola vez más. Las llamadas dan su
// propio resultado para «sesión vencida», «sin conexión» y «error inesperado».
suspend fun <T : Any> AuthRepository.conAccess(
    vencida: T,
    sinConexion: T,
    inesperado: T,
    pedir: suspend (access: String) -> T?,
): T {
    accessToken?.let { access -> pedir(access)?.let { return it } }
    return when (renovar()) {
        // Un 401 con un access recién emitido no es una sesión vencida: es otro problema.
        ResultadoRenovacion.Exito -> accessToken?.let { pedir(it) } ?: inesperado
        ResultadoRenovacion.SesionExpirada -> vencida
        ResultadoRenovacion.SinConexion -> sinConexion
        ResultadoRenovacion.ErrorInesperado -> inesperado
    }
}
