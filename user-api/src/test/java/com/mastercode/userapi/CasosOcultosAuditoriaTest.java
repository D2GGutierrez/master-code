package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mastercode.userapi.admin.ProblemaCalificableDto;
import com.mastercode.userapi.admin.ProblemaDto;
import com.mastercode.userapi.sandbox.EjecucionResultado;
import com.mastercode.userapi.sandbox.EjecucionSolicitud;
import com.mastercode.userapi.sandbox.Estado;
import com.mastercode.userapi.sandbox.LenguajeNoSoportadoException;
import com.mastercode.userapi.sandbox.SandboxNoDisponibleException;

// HU39-T3: auditoría de casos ocultos. Cubre lo que EnviosApiTest no cubre: un tiempo agotado, una memoria excedida,
// un fallo del sandbox y un 400 justo en el caso oculto, y la salida oculta guardada (HU37-T1) leída después por el
// historial (HU41) y el detalle (HU42). En todos el motor «repite» la entrada oculta: si algo la reenvía, la marca sale.
class CasosOcultosAuditoriaTest extends PruebaDeApi {

    private static final long CURSO = 10;
    private static final long PROBLEMA = 7;
    private static final String CODIGO = "print(input())";
    // Marca inventada: solo puede aparecer en una respuesta si viajó el caso oculto.
    private static final String MARCA = "MARCA-OCULTA-123";

    private static final ProblemaCalificableDto.Caso PUBLICO = new ProblemaCalificableDto.Caso(1, "2 3", "5", true);
    private static final ProblemaCalificableDto.Caso OCULTO = new ProblemaCalificableDto.Caso(2, MARCA, MARCA, false);

    @Autowired
    private ObjectMapper json;

    private String alumnoInscrito() {
        String email = correoUnico();
        inscribir(crearEstudiante(email), CURSO);
        when(admin.problemaParaCalificar(PROBLEMA)).thenReturn(
                new ProblemaCalificableDto(PROBLEMA, CURSO, "python", "escritura_codigo", List.of(PUBLICO, OCULTO)));
        return email;
    }

    // El caso público pasa; en el oculto el motor responde con el estado dado y un error que repite la entrada.
    private void ocultoTermina(Estado estado) {
        when(sandbox.ejecutar(any())).thenAnswer(llamada -> {
            EjecucionSolicitud solicitud = llamada.getArgument(0);
            if ("2 3".equals(solicitud.entrada())) {
                return new EjecucionResultado(Estado.OK, "5", "", 5L);
            }
            return new EjecucionResultado(estado, solicitud.entrada(), "falló con " + solicitud.entrada(), null);
        });
    }

    private ResultActions enviar(String email) throws Exception {
        return mvc.perform(post("/api/problemas/" + PROBLEMA + "/envios")
                .with(tokenDe(email, "estudiante"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("codigo", CODIGO))));
    }

    private static String texto(ResultActions respuesta) throws Exception {
        return respuesta.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void un_tiempo_agotado_en_el_caso_oculto_no_filtra_su_entrada() throws Exception {
        String yo = alumnoInscrito();
        ocultoTermina(Estado.TIEMPO_AGOTADO);

        assertThat(texto(enviar(yo).andExpect(status().isCreated()))).doesNotContain(MARCA);
    }

    @Test
    void una_memoria_excedida_en_el_caso_oculto_no_filtra_su_entrada() throws Exception {
        String yo = alumnoInscrito();
        ocultoTermina(Estado.MEMORIA_EXCEDIDA);

        assertThat(texto(enviar(yo).andExpect(status().isCreated()))).doesNotContain(MARCA);
    }

    @Test
    void un_error_del_servicio_en_el_caso_oculto_da_503_sin_su_entrada() throws Exception {
        String yo = alumnoInscrito();
        ocultoTermina(Estado.ERROR_SERVICIO);

        assertThat(texto(enviar(yo).andExpect(status().isServiceUnavailable()))).doesNotContain(MARCA);
    }

    @Test
    void un_sandbox_caido_en_el_caso_oculto_da_503_sin_el_mensaje_de_la_excepcion() throws Exception {
        String yo = alumnoInscrito();
        when(sandbox.ejecutar(any())).thenAnswer(llamada -> {
            EjecucionSolicitud solicitud = llamada.getArgument(0);
            if ("2 3".equals(solicitud.entrada())) {
                return new EjecucionResultado(Estado.OK, "5", "", 5L);
            }
            // Como un 422 de ai-service, que repite el campo que rechazó.
            throw new SandboxNoDisponibleException("ai-service rechazó la entrada " + solicitud.entrada(), null);
        });

        assertThat(texto(enviar(yo).andExpect(status().isServiceUnavailable()))).doesNotContain(MARCA);
    }

    @Test
    void un_400_en_el_caso_oculto_no_filtra_su_entrada() throws Exception {
        String yo = alumnoInscrito();
        when(sandbox.ejecutar(any())).thenAnswer(llamada -> {
            EjecucionSolicitud solicitud = llamada.getArgument(0);
            if ("2 3".equals(solicitud.entrada())) {
                return new EjecucionResultado(Estado.OK, "5", "", 5L);
            }
            throw new LenguajeNoSoportadoException(solicitud.entrada());
        });

        assertThat(texto(enviar(yo).andExpect(status().isBadRequest()))).doesNotContain(MARCA);
    }

    @Test
    void la_salida_oculta_guardada_no_sale_por_el_historial_ni_por_el_detalle() throws Exception {
        String yo = alumnoInscrito();
        // El programa imprime lo que lee: en el caso oculto pasa e imprime la marca, que queda guardada (HU37-T1).
        when(sandbox.ejecutar(any())).thenAnswer(llamada -> {
            EjecucionSolicitud solicitud = llamada.getArgument(0);
            String salida = "2 3".equals(solicitud.entrada()) ? "5" : solicitud.entrada();
            return new EjecucionResultado(Estado.OK, salida, "", 5L);
        });
        when(admin.problema(PROBLEMA)).thenReturn(new ProblemaDto(PROBLEMA, "Eco", "Imprime lo que lees.", "python",
                "facil", "web", "escritura_codigo", "", List.of(new ProblemaDto.Caso("2 3", "5")), CURSO));

        long envio = json.readTree(texto(enviar(yo).andExpect(status().isCreated()))).get("id").asLong();

        // Control: la marca sí está guardada, así que las dos lecturas de abajo podrían filtrarla.
        String guardada = jdbc.sql("""
                SELECT salida_obtenida FROM app_schema.app_resultado_caso_prueba
                WHERE envio_id = ? AND caso_prueba_id = ?
                """).params(envio, OCULTO.id()).query(String.class).single();
        assertThat(guardada).isEqualTo(MARCA);

        String historial = texto(mvc.perform(get("/api/problemas/" + PROBLEMA + "/envios")
                .with(tokenDe(yo, "estudiante"))).andExpect(status().isOk()));
        String detalle = texto(mvc.perform(get("/api/envios/" + envio)
                .with(tokenDe(yo, "estudiante"))).andExpect(status().isOk()));
        assertThat(historial).doesNotContain(MARCA);
        assertThat(detalle).doesNotContain(MARCA);
    }
}
