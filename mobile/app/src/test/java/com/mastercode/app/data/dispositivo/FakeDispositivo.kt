package com.mastercode.app.data.dispositivo

// Fakes de HU52-T1: la API, Firebase (el proveedor del token) y el guardado del token pendiente.
class FakeDispositivoApi : DispositivoApi {
    var alRegistrar: suspend (token: String) -> Unit = {}
    val tokensRecibidos = mutableListOf<String>()
    val accessRecibidos = mutableListOf<String>()

    override suspend fun registrar(access: String, token: String) {
        accessRecibidos += access
        tokensRecibidos += token
        alRegistrar(token)
    }
}

class FakeProveedorDeToken(var token: String? = null) : ProveedorDeToken {
    var veces = 0

    override suspend fun obtener(): String? {
        veces++
        return token
    }
}

class MemoriaToken : AlmacenDeToken {
    var pendiente: String? = null

    override fun leerPendiente(): String? = pendiente

    override fun guardarPendiente(token: String?) {
        pendiente = token
    }
}
