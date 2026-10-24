package com.mastercode.app.data.cursos

import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.ErrorHttp
import com.mastercode.app.data.auth.FakeAuthApi
import com.mastercode.app.data.auth.MemoriaTokenStorage
import com.mastercode.app.data.auth.Tokens
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.time.LocalDate

// HttpMisCursosApi real contra un servidor falso del JDK, como LoginMovilEndToEndTest.
// El JSON copia el formato de GET /api/mis-cursos (MisCursosApiTest de user-api). El token es inventado.
class MisCursosApiTest {

    private lateinit var servidor: HttpServer
    private var metodo: String? = null
    private var ruta: String? = null
    private var authorization: String? = null
    private var codigoRespuesta = 200
    private var cuerpoRespuesta = """
        [
          {"curso_id": 11, "nombre": "Java Básico", "lenguaje": "java", "institucion": "ejemplo.edu.pe",
           "fecha_inscripcion": "2026-10-27T09:15:00.123-05:00"},
          {"curso_id": 10, "nombre": "Algoritmos I", "lenguaje": "python", "institucion": "ejemplo.edu.pe",
           "fecha_inscripcion": "2026-10-20T18:00:00Z"}
        ]
    """.trimIndent()

    @Before
    fun levantarServidor() {
        servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servidor.createContext("/") { intercambio ->
            metodo = intercambio.requestMethod
            ruta = intercambio.requestURI.path
            authorization = intercambio.requestHeaders.getFirst("Authorization")
            val bytes = cuerpoRespuesta.toByteArray()
            intercambio.sendResponseHeaders(codigoRespuesta, bytes.size.toLong())
            intercambio.responseBody.use { it.write(bytes) }
        }
        servidor.start()
    }

    @After
    fun apagarServidor() {
        servidor.stop(0)
    }

    private fun api() = HttpMisCursosApi("http://127.0.0.1:${servidor.address.port}/")

    @Test
    fun pideMisCursosConElBearerYLeeCadaCurso() = runTest {
        val cursos = api().misCursos("access-falso")

        assertEquals("GET", metodo)
        assertEquals("/api/mis-cursos", ruta)
        assertEquals("Bearer access-falso", authorization)
        assertEquals(
            listOf(
                CursoInscrito(11, "Java Básico", "java", LocalDate.of(2026, 10, 27)),
                CursoInscrito(10, "Algoritmos I", "python", LocalDate.of(2026, 10, 20)),
            ),
            cursos,
        )
    }

    @Test
    fun unaListaVaciaDevuelveCeroCursos() = runTest {
        cuerpoRespuesta = "[]"

        assertEquals(emptyList<CursoInscrito>(), api().misCursos("access-falso"))
    }

    @Test
    fun sinFechaElCursoSeLeeIgual() = runTest {
        cuerpoRespuesta = """[{"curso_id": 10, "nombre": "Algoritmos I", "lenguaje": "python"}]"""

        assertNull(api().misCursos("access-falso").single().fechaInscripcion)
    }

    @Test
    fun un401DeUserApiSinRenovacionPosibleEsSesionVencida() = runTest {
        codigoRespuesta = 401
        cuerpoRespuesta = """{"detail": "Token inválido."}"""
        // admin-api también rechaza el refresh: no hay forma de seguir.
        val authApi = FakeAuthApi().apply {
            respuestaLogin = { Tokens(access = "access-vencido", refresh = "refresh-1") }
            respuestaRenovar = { throw ErrorHttp(401) }
        }
        val auth = AuthRepository(authApi, MemoriaTokenStorage())
        auth.login("ana@ejemplo.edu.pe", "clave-inventada")

        val resultado = MisCursosRepository(api(), auth).misCursos()

        assertEquals(ResultadoMisCursos.SesionVencida, resultado)
        assertEquals("Bearer access-vencido", authorization)
    }
}
