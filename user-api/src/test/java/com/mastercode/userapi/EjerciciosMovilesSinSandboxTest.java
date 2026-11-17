package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

// HU48-T1 (CA-48.1): ningún ejercicio del celular (predecir la salida, opción múltiple, completar el espacio) llama al
// sandbox ni a ai-service al evaluarse: se corrige directamente en user-api. Se comprueba de dos maneras: respondiendo
// los tres tipos (acertando y fallando) y mirando que el mock del sandbox, que es el único camino hacia ai-service
// (/execute), no recibe ninguna llamada; y revisando el código del paquete de corrección y de las rutas del Dojo.
class EjerciciosMovilesSinSandboxTest extends EjerciciosMovilesBase {

    @Test
    void responder_los_tres_tipos_no_toca_el_sandbox() throws Exception {
        String yo = alumno();
        Ejercicio prediccion = prediccion(7, "facil", "Bucles", "ok");
        Ejercicio multiple = multiple(8, "facil", "Listas");
        Ejercicio completar = completar(9, "facil", "Cadenas", "len");
        publicar(prediccion, multiple, completar);

        responder(yo, 7, "ok").andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("correcto"));
        responder(yo, 7, "mal").andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("incorrecto"));
        enviar(yo, 8, null, correctaDe(multiple), null, null).andExpect(status().isOk());
        enviar(yo, 8, null, 801L, null, null).andExpect(status().isOk());
        responder(yo, 9, "LEN").andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("correcto"));
        responder(yo, 9, "otra").andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("incorrecto"));

        verifyNoInteractions(sandbox);
    }

    @Test
    void jugar_una_sesion_del_dojo_completa_no_toca_el_sandbox() throws Exception {
        String yo = alumno();
        publicar(prediccion(7, "facil", "Bucles", "ok"), multiple(8, "facil", "Listas"), completar(9, "facil", "Cadenas", "len"));
        long sesion = sesion(yo);

        for (int i = 0; i < 3; i++) {
            long problema = idDelSiguiente(yo, sesion);
            enviar(yo, problema, "ok", null, sesion, null).andExpect(status().is(org.hamcrest.Matchers.anyOf(
                    org.hamcrest.Matchers.is(200), org.hamcrest.Matchers.is(400))));
        }
        mvcTerminar(yo, sesion);

        verifyNoInteractions(sandbox);
    }

    private void mvcTerminar(String email, long sesion) throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/dojo/sesiones/" + sesion + "/terminar").with(tokenDeCliente(email, "mobile")))
                .andExpect(status().isOk());
    }

    @Test
    void el_codigo_de_la_correccion_y_del_dojo_no_menciona_el_sandbox_ni_ai_service() throws IOException {
        for (String paquete : List.of("ejercicio", "dojo", "cinturon")) {
            Path carpeta = carpetaDeCodigo(paquete);
            try (Stream<Path> archivos = Files.walk(carpeta)) {
                for (Path archivo : archivos.filter(p -> p.toString().endsWith(".java")).toList()) {
                    // Solo el código: los comentarios pueden decir que NO usa el sandbox.
                    String texto = Files.readAllLines(archivo).stream()
                            .map(linea -> linea.replaceAll("//.*", ""))
                            .filter(linea -> !linea.trim().startsWith("*") && !linea.trim().startsWith("/*"))
                            .collect(java.util.stream.Collectors.joining("\n")).toLowerCase();
                    assertThat(texto).as("%s no debe usar el sandbox ni ai-service", archivo.getFileName())
                            .doesNotContain("sandbox").doesNotContain("ai-service").doesNotContain("ejecucion");
                }
            }
        }
    }

    // Las pruebas corren desde user-api (Maven) o desde la raíz del monorepo.
    private static Path carpetaDeCodigo(String paquete) {
        Path desdeUserApi = Path.of("src/main/java/com/mastercode/userapi", paquete);
        return Files.isDirectory(desdeUserApi) ? desdeUserApi : Path.of("user-api").resolve(desdeUserApi);
    }
}
