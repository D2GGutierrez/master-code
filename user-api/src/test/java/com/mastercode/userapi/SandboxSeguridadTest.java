package com.mastercode.userapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.mastercode.userapi.sandbox.EjecucionResultado;
import com.mastercode.userapi.sandbox.EjecucionSolicitud;
import com.mastercode.userapi.sandbox.Estado;
import com.mastercode.userapi.sandbox.SandboxCliente;

// HU07-T5: el sandbox corta un bucle infinito y bloquea archivos del sistema, memoria excesiva y red.
// Va por el camino real (ADR-7 D1): SandboxCliente → ai-service → Judge0, con los límites de HU07-T2
// (docker-compose.yml, judge0-server): CPU 5 s, tiempo total 10 s, memoria 256000 KB, sin red.
// Sin un Judge0 que ejecute de verdad, las pruebas se saltan solas (así pasa en el CI).
// Para correrlas, en Linux con cgroup v1 (Judge0 1.13.1 no ejecuta código con cgroup v2 / Docker Desktop):
//   SANDBOX_PROVIDER=judge0 docker compose up -d judge0-server judge0-worker ai-service
//   cd user-api && ./mvnw test -Dtest=SandboxSeguridadTest
@Tag("sandbox")
class SandboxSeguridadTest {

    private static final String URL = urlDeAiService();
    // WALL_TIME_LIMIT de HU07-T2, más un margen para lo que Judge0 tarda en cortar el proceso.
    private static final long LIMITE_TOTAL_MS = 10_000;
    private static final long MARGEN_MS = 2_000;
    // Tope de cada prueba en este lado: si el sandbox no corta, la prueba falla en vez de quedarse colgada.
    private static final Duration TOPE_PRUEBA = Duration.ofSeconds(60);

    private static final SandboxCliente cliente = crearCliente(Duration.ofSeconds(5), Duration.ofSeconds(40));
    private static boolean haySandboxReal;

    // Misma propiedad que sandbox.url en application.yml, con el mismo valor por defecto.
    private static String urlDeAiService() {
        String url = System.getenv("AI_SERVICE_URL");
        return url == null || url.isBlank() ? "http://localhost:8002" : url;
    }

    private static SandboxCliente crearCliente(Duration conexion, Duration lectura) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(conexion);
        fabrica.setReadTimeout(lectura);
        return new SandboxCliente(RestClient.builder().baseUrl(URL).requestFactory(fabrica).build());
    }

    // Se salta si ai-service está apagado, usa el proveedor mock o Judge0 no ejecuta. La entrada va
    // vacía a propósito: el mock devuelve la entrada como salida, así nunca responde "ok" por su cuenta.
    // Se comprueba una vez; la suposición va en cada prueba para que el informe las cuente como saltadas.
    @BeforeAll
    static void comprobarQueHayUnSandboxReal() {
        try {
            EjecucionResultado control = crearCliente(Duration.ofSeconds(2), Duration.ofSeconds(15))
                    .ejecutar(new EjecucionSolicitud("print(\"ok\")", "python", ""));
            haySandboxReal = control.estado() == Estado.OK && "ok".equals(control.salida().strip());
        } catch (RuntimeException e) {
            haySandboxReal = false;
        }
    }

    @BeforeEach
    void saltarSinSandboxReal() {
        assumeTrue(haySandboxReal, "No hay un sandbox real en " + URL + ": se saltan las pruebas de seguridad");
    }

    private static EjecucionResultado ejecutarPython(String codigo) {
        return assertTimeoutPreemptively(TOPE_PRUEBA,
                () -> cliente.ejecutar(new EjecucionSolicitud(codigo, "python", "")));
    }

    // Un fallo del motor no demuestra que el sandbox bloqueó nada: nunca cuenta como resultado válido.
    private static void noEsFallaDelServicio(EjecucionResultado resultado) {
        assertNotEquals(Estado.ERROR_SERVICIO, resultado.estado(), "Falló el motor, no el programa: " + resultado.error());
    }

    @Test
    void un_bucle_infinito_se_corta_por_tiempo_agotado() {
        EjecucionResultado resultado = ejecutarPython("while True:\n    pass\n");

        assertEquals(Estado.TIEMPO_AGOTADO, resultado.estado());
        // Secundario: tiempo_ms puede venir vacío (contrato de POST /execute).
        if (resultado.tiempoMs() != null) {
            assertTrue(resultado.tiempoMs() <= LIMITE_TOTAL_MS + MARGEN_MS,
                    "El programa corrió " + resultado.tiempoMs() + " ms, más que el límite configurado");
        }
    }

    @Test
    void un_programa_que_termina_es_aceptado_con_su_salida() {
        EjecucionResultado resultado = ejecutarPython("print(\"ok\")\n");

        assertEquals(Estado.OK, resultado.estado());
        assertEquals("ok", resultado.salida().strip());
    }

    @Test
    void leer_etc_passwd_no_muestra_el_archivo_del_sistema() {
        EjecucionResultado resultado = ejecutarPython("with open(\"/etc/passwd\") as f:\n    print(f.read())\n");

        noEsFallaDelServicio(resultado);
        assertFalse(resultado.salida().contains("root:"), "La salida muestra /etc/passwd");
        assertFalse(resultado.error().contains("root:"), "El error muestra /etc/passwd");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/etc/prueba_sandbox.txt", "/usr/prueba_sandbox.txt"})
    void escribir_en_una_ruta_del_sistema_falla(String ruta) {
        EjecucionResultado resultado = ejecutarPython(
                "with open(\"" + ruta + "\", \"w\") as f:\n    f.write(\"sandbox\")\nprint(\"escrito\")\n");

        noEsFallaDelServicio(resultado);
        assertNotEquals(Estado.OK, resultado.estado(), "El programa pudo escribir en " + ruta);
        assertFalse(resultado.salida().contains("escrito"));
    }

    // ai-service traduce los estados 7 a 12 de Judge0 a ERROR_EJECUCION y el cliente no devuelve la
    // memoria usada: no se puede distinguir «memoria excedida», solo que el programa no terminó bien.
    @Test
    void reservar_memoria_sin_parar_no_termina_bien() {
        EjecucionResultado resultado = ejecutarPython(
                "datos = []\nwhile True:\n    datos.append(bytearray(10 * 1024 * 1024))\n");

        noEsFallaDelServicio(resultado);
        assertNotEquals(Estado.OK, resultado.estado());
    }

    // A una IP y no a un nombre: así no basta con que falle el DNS, tiene que fallar la conexión misma.
    @Test
    void conectarse_a_un_host_publico_falla_sin_red() {
        EjecucionResultado resultado = ejecutarPython("""
                import socket
                s = socket.create_connection(("1.1.1.1", 80), timeout=3)
                s.sendall(b"HEAD / HTTP/1.0\\r\\nHost: 1.1.1.1\\r\\n\\r\\n")
                print(s.recv(100).decode(errors="replace"))
                """);

        noEsFallaDelServicio(resultado);
        assertNotEquals(Estado.OK, resultado.estado(), "El programa abrió una conexión de red");
        assertFalse(resultado.salida().contains("HTTP/"), "El programa recibió datos de la red");
    }
}
