package com.mastercode.app.data.dispositivo

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

// HU52-T1 (CA-52.2): la app registra en user-api el token del teléfono al abrir con sesión y cuando Firebase lo renueva,
// y no pierde un token por falta de señal. Firebase se reemplaza por FakeProveedorDeToken.
class DispositivoRepositoryTest {

    private val api = FakeDispositivoApi()
    private val proveedor = FakeProveedorDeToken("token-de-firebase")
    private val almacen = MemoriaToken()
    private val auth = AuthRepository(FakeAuthApi(), MemoriaTokenStorage(refresh = "refresh-guardado"))

    private fun repositorio() = DispositivoRepository(api, auth, proveedor, almacen)

    @Test
    fun alAbrirConSesionRegistraElTokenQueDaFirebase() = runTest {
        val resultado = repositorio().registrarElDelTelefono()

        assertEquals(ResultadoDispositivo.Registrado, resultado)
        assertEquals(listOf("token-de-firebase"), api.tokensRecibidos)
        assertNull(almacen.pendiente)
    }

    // Sin el SDK de Firebase no hay token: no se llama a user-api.
    @Test
    fun sinTokenDeFirebaseNoSeMandaNada() = runTest {
        val repositorio = DispositivoRepository(api, auth, SinFirebase, almacen)

        assertEquals(ResultadoDispositivo.SinToken, repositorio.registrarElDelTelefono())
        assertEquals(emptyList<String>(), api.tokensRecibidos)
    }

    @Test
    fun siFirebaseTodaviaNoTieneTokenNoSeMandaNada() = runTest {
        proveedor.token = null

        assertEquals(ResultadoDispositivo.SinToken, repositorio().registrarElDelTelefono())
        assertEquals(emptyList<String>(), api.tokensRecibidos)
    }

    @Test
    fun alRenovarseElTokenSeRegistraElNuevo() = runTest {
        val resultado = repositorio().alRenovarseElToken("token-nuevo")

        assertEquals(ResultadoDispositivo.Registrado, resultado)
        assertEquals(listOf("token-nuevo"), api.tokensRecibidos)
    }

    // Sin señal el token no se pierde: queda guardado y se manda en la próxima apertura, aunque Firebase dé otro.
    @Test
    fun sinConexionElTokenQuedaPendienteYSeReintentaDespues() = runTest {
        api.alRegistrar = { throw IOException("sin red") }

        val primero = repositorio().alRenovarseElToken("token-nuevo")

        assertEquals(ResultadoDispositivo.SinConexion, primero)
        assertEquals("token-nuevo", almacen.pendiente)

        api.alRegistrar = {}
        api.tokensRecibidos.clear()
        val segundo = repositorio().registrarElDelTelefono()

        assertEquals(ResultadoDispositivo.Registrado, segundo)
        assertEquals(listOf("token-nuevo"), api.tokensRecibidos)
        assertNull(almacen.pendiente)
        // Mandó el pendiente: no hizo falta preguntarle a Firebase.
        assertEquals(0, proveedor.veces)
    }

    @Test
    fun conLaSesionVencidaElTokenQuedaPendienteParaCuandoVuelvaAIniciarSesion() = runTest {
        val auth401 = FakeAuthApi().apply { respuestaRenovar = { throw ErrorHttp(401) } }
        val repositorio = DispositivoRepository(
            api, AuthRepository(auth401, MemoriaTokenStorage(refresh = "vencido")), proveedor, almacen,
        )

        val resultado = repositorio.alRenovarseElToken("token-nuevo")

        assertEquals(ResultadoDispositivo.SesionVencida, resultado)
        assertEquals("token-nuevo", almacen.pendiente)
        assertEquals(emptyList<String>(), api.tokensRecibidos)
    }

    // Un 401 del access (vencido) se resuelve renovándolo: el token llega igual y no queda pendiente.
    @Test
    fun siElAccessVencioSeRenuevaYElTokenLlega() = runTest {
        // Con un access ya en memoria que user-api rechaza: se renueva con el refresh y se pide una sola vez más.
        auth.renovar()
        var llamadas = 0
        api.alRegistrar = { if (llamadas++ == 0) throw ErrorHttp(401) }

        val resultado = repositorio().registrarElDelTelefono()

        assertEquals(ResultadoDispositivo.Registrado, resultado)
        assertEquals(2, api.tokensRecibidos.size)
        assertNull(almacen.pendiente)
    }

    // 400: user-api no acepta ese token; reintentarlo siempre fallaría, así que se descarta.
    @Test
    fun unTokenRechazadoPorElServidorSeDescartaYNoSeReintenta() = runTest {
        api.alRegistrar = { throw ErrorHttp(400) }

        val resultado = repositorio().alRenovarseElToken("token-malo")

        assertEquals(ResultadoDispositivo.TokenRechazado, resultado)
        assertNull(almacen.pendiente)
    }

    @Test
    fun unErrorDelServidorDejaElTokenPendiente() = runTest {
        api.alRegistrar = { throw ErrorHttp(503) }

        val resultado = repositorio().alRenovarseElToken("token-nuevo")

        assertEquals(ResultadoDispositivo.ErrorInesperado, resultado)
        assertEquals("token-nuevo", almacen.pendiente)
    }

    // Un token nuevo que llega mientras había uno pendiente lo reemplaza: solo importa el último que dio Firebase.
    @Test
    fun unTokenNuevoReemplazaAlPendiente() = runTest {
        almacen.pendiente = "token-viejo"
        api.alRegistrar = { throw IOException("sin red") }
        repositorio().alRenovarseElToken("token-nuevo")
        assertEquals("token-nuevo", almacen.pendiente)

        api.alRegistrar = {}
        api.tokensRecibidos.clear()
        repositorio().registrarElDelTelefono()

        assertEquals(listOf("token-nuevo"), api.tokensRecibidos)
    }

    @Test
    fun registrarVariasVecesElMismoTokenEsInofensivo() = runTest {
        val repositorio = repositorio()

        repositorio.registrarElDelTelefono()
        repositorio.registrarElDelTelefono()

        assertEquals(listOf("token-de-firebase", "token-de-firebase"), api.tokensRecibidos)
        assertNull(almacen.pendiente)
    }
}
