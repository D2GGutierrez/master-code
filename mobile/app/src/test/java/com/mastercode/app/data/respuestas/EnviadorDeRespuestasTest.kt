package com.mastercode.app.data.respuestas

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import com.mastercode.app.data.problemas.FakeProblemaApi
import com.mastercode.app.data.problemas.ProblemaRepository
import com.mastercode.app.data.problemas.RespuestaDelEstudiante
import com.mastercode.app.data.problemas.ResultadoDeEnvio
import com.mastercode.app.data.problemas.ResultadoDeRespuesta
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

// HU48-T3 (CA-48.4, CA-48.5): sin conexión la respuesta queda guardada en el teléfono y se reenvía al volver la señal,
// registrándose una sola vez. Que se registre una sola vez lo garantiza user-api con el intento_id (IntentosMovilApiTest);
// aquí se prueba que la app guarda, reenvía con el MISMO intento_id y no reenvía lo ya enviado.
class EnviadorDeRespuestasTest {

    private val api = FakeProblemaApi()
    private val almacen = MemoriaPendientes()
    private val auth = AuthRepository(FakeAuthApi(), MemoriaTokenStorage(refresh = "refresh-guardado"))
    private val enviador = EnviadorDeRespuestas(ProblemaRepository(api, auth), almacen)

    private val respuesta = RespuestaDelEstudiante(7, "5", null, null, "intento-1")
    private val acierto = ResultadoDeRespuesta(acerto = true, respuestaCorrecta = null, intentosRestantes = 2)

    private fun sinConexion() {
        api.alResponder = { _, _ -> throw IOException("sin red") }
    }

    @Test
    fun conConexionDevuelveLaCorreccionYNoGuardaNada() = runTest {
        api.alResponder = { _, _ -> acierto }

        assertEquals(SalidaDeEnvio.Enviada(acierto), enviador.enviar(respuesta))
        assertEquals(emptyList<RespuestaDelEstudiante>(), almacen.guardadas)
    }

    @Test
    fun sinConexionLaRespuestaQuedaGuardadaEnElTelefono() = runTest {
        sinConexion()

        assertEquals(SalidaDeEnvio.Guardada, enviador.enviar(respuesta))
        assertEquals(listOf(respuesta), almacen.guardadas)
        assertEquals(respuesta, enviador.pendienteDe(7))
    }

    @Test
    fun alVolverLaSenalSeReenviaConElMismoIntentoIdYSeLimpiaLoGuardado() = runTest {
        sinConexion()
        enviador.enviar(respuesta)
        api.respuestasRecibidas.clear()
        api.alResponder = { _, _ -> acierto }

        enviador.reenviarPendientes()

        assertEquals(listOf(respuesta), api.respuestasRecibidas)
        assertEquals(emptyList<RespuestaDelEstudiante>(), almacen.guardadas)
        assertEquals(null, enviador.pendienteDe(7))
    }

    // CA-48.5: tras reenviar una vez, otro aviso de señal no vuelve a mandar lo mismo.
    @Test
    fun unaRespuestaYaReenviadaNoSeVuelveAMandar() = runTest {
        sinConexion()
        enviador.enviar(respuesta)
        api.respuestasRecibidas.clear()
        api.alResponder = { _, _ -> acierto }

        enviador.reenviarPendientes()
        enviador.reenviarPendientes()

        assertEquals(1, api.respuestasRecibidas.size)
    }

    @Test
    fun siSigueSinSenalLaRespuestaSigueGuardada() = runTest {
        sinConexion()
        enviador.enviar(respuesta)

        enviador.reenviarPendientes()

        assertEquals(listOf(respuesta), almacen.guardadas)
    }

    @Test
    fun reintentarElMismoIntentoSinSenalNoLoDuplicaEnLoGuardado() = runTest {
        sinConexion()

        enviador.enviar(respuesta)
        enviador.enviar(respuesta)

        assertEquals(listOf(respuesta), almacen.guardadas)
    }

    @Test
    fun reenviaEnElOrdenEnQueSeDieronYSeDetieneEnLaPrimeraQueFalla() = runTest {
        val segunda = respuesta.copy(problemaId = 8, intentoId = "intento-2")
        val tercera = respuesta.copy(problemaId = 9, intentoId = "intento-3")
        sinConexion()
        enviador.enviar(respuesta)
        enviador.enviar(segunda)
        enviador.enviar(tercera)
        api.respuestasRecibidas.clear()
        // La señal se va otra vez a la segunda: la primera sale, las otras quedan.
        api.alResponder = { _, r -> if (r.intentoId == "intento-1") acierto else throw IOException("sin red") }

        enviador.reenviarPendientes()

        assertEquals(listOf("intento-1", "intento-2"), api.respuestasRecibidas.map { it.intentoId })
        assertEquals(listOf(segunda, tercera), almacen.guardadas)
    }

    // El servidor la rechazó para siempre (por ejemplo, ya no había intentos): reenviarla no sirve, se descarta.
    @Test
    fun unaRespuestaQueElServidorRechazaSeDescartaYSeAvisa() = runTest {
        sinConexion()
        enviador.enviar(respuesta)
        api.alResponder = { _, _ -> throw ErrorHttp(409) }
        // Se empieza a escuchar antes de reenviar, como hace la pantalla abierta.
        val aviso = async(start = CoroutineStart.UNDISPATCHED) { enviador.resueltas.first() }

        enviador.reenviarPendientes()

        assertEquals(emptyList<RespuestaDelEstudiante>(), almacen.guardadas)
        assertEquals(PendienteResuelto(respuesta, ResultadoDeEnvio.Conflicto), aviso.await())
    }

    @Test
    fun unErrorDelServidorAlReenviarDejaLaRespuestaGuardadaParaDespues() = runTest {
        sinConexion()
        enviador.enviar(respuesta)
        api.alResponder = { _, _ -> throw ErrorHttp(500) }

        enviador.reenviarPendientes()

        assertEquals(listOf(respuesta), almacen.guardadas)
    }

    @Test
    fun alReenviarAvisaElResultadoParaQueLaPantallaLoMuestre() = runTest {
        sinConexion()
        enviador.enviar(respuesta)
        api.alResponder = { _, _ -> acierto }
        val aviso = async(start = CoroutineStart.UNDISPATCHED) { enviador.resueltas.first() }

        enviador.reenviarPendientes()

        assertEquals(PendienteResuelto(respuesta, ResultadoDeEnvio.Enviada(acierto)), aviso.await())
    }

    @Test
    fun unaSesionVencidaNoGuardaLaRespuesta() = runTest {
        api.alResponder = { _, _ -> throw ErrorHttp(401) }
        val auth401 = FakeAuthApi().apply { respuestaRenovar = { throw ErrorHttp(401) } }
        val enviadorSinSesion = EnviadorDeRespuestas(
            ProblemaRepository(api, AuthRepository(auth401, MemoriaTokenStorage(refresh = "vencido"))),
            almacen,
        )

        assertEquals(SalidaDeEnvio.SesionVencida, enviadorSinSesion.enviar(respuesta))
        assertEquals(emptyList<RespuestaDelEstudiante>(), almacen.guardadas)
    }

    @Test
    fun loQueElServidorRechazaAlEnviarNoSeGuarda() = runTest {
        api.alResponder = { _, _ -> throw ErrorHttp(409) }

        assertEquals(SalidaDeEnvio.Fallida(ResultadoDeEnvio.Conflicto), enviador.enviar(respuesta))
        assertEquals(emptyList<RespuestaDelEstudiante>(), almacen.guardadas)
    }
}
