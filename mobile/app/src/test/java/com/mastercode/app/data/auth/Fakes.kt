package com.mastercode.app.data.auth

// API falsa: cada prueba decide qué responde (o qué lanza) el servidor.
class FakeAuthApi : AuthApi {
    var respuestaLogin: suspend () -> Tokens = { Tokens(access = "access-1", refresh = "refresh-1") }
    var respuestaRenovar: suspend () -> Tokens = { Tokens(access = "access-2", refresh = "refresh-2") }
    var refreshRecibido: String? = null

    override suspend fun login(email: String, password: String): Tokens = respuestaLogin()

    override suspend fun renovar(refresh: String): Tokens {
        refreshRecibido = refresh
        return respuestaRenovar()
    }
}

// Reemplaza a Keystore, que no existe en la JVM de las pruebas.
class MemoriaTokenStorage(var refresh: String? = null) : TokenStorage {
    override fun guardarRefresh(refresh: String) {
        this.refresh = refresh
    }

    override fun leerRefresh(): String? = refresh

    override fun borrar() {
        refresh = null
    }
}
