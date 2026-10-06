package com.mastercode.app.data.auth

// Dónde se guarda el refresh token. Solo el refresh: el access vive en memoria.
interface TokenStorage {
    fun guardarRefresh(refresh: String)
    fun leerRefresh(): String?
    fun borrar()
}
