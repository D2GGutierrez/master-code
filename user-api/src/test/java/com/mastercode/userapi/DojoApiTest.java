package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.CursoNoExisteException;
import com.mastercode.userapi.admin.ProblemaDto;
import com.mastercode.userapi.admin.ProblemaNoVisibleException;
import com.mastercode.userapi.admin.ProblemaResumenDto;

// HU53-T2 (CA-53.2, CA-53.3): iniciar una sesión del Dojo y pedir el siguiente ejercicio sin repetir. admin-api es un mock
// que dice qué ejercicios son visibles en cada curso.
class DojoApiTest extends PruebaDeApi {

    private static final long CURSO = 10;
    private static final long OTRO_CURSO = 11;
    private static final String RESPUESTA_SECRETA = "RESPUESTA-SECRETA-5530";

    private static ProblemaResumenDto resumen(long id, String lenguaje, String tipo) {
        return new ProblemaResumenDto(id, "Ejercicio " + id, lenguaje, "facil", "movil", tipo, null);
    }

    private static ProblemaDto problema(long id, String lenguaje, String tipo, long curso) {
        return new ProblemaDto(id, "Ejercicio " + id, "¿Qué imprime? " + id, lenguaje, "facil", "movil", tipo, "",
                List.of(), curso);
    }

    // Deja visibles en el curso estos ejercicios, y sus detalles para cuando se entreguen.
    private void publicar(long curso, ProblemaDto... problemas) {
        when(admin.problemasDelCurso(curso, "movil")).thenReturn(List.of(problemas).stream()
                .map(p -> resumen(p.id(), p.lenguaje(), p.tipoEjercicio())).toList());
        for (ProblemaDto problema : problemas) {
            when(admin.problema(problema.id())).thenReturn(problema);
        }
    }

    private String alumnoInscritoEn(long... cursos) {
        String email = correoUnico();
        long id = crearEstudiante(email);
        for (long curso : cursos) {
            inscribir(id, curso);
        }
        return email;
    }

    private ResultActions iniciar(String email, String cuerpo) throws Exception {
        return mvc.perform(post("/api/dojo/sesiones").with(tokenDeCliente(email, "mobile"))
                .contentType(APPLICATION_JSON).content(cuerpo));
    }

    private long iniciarSesion(String email, String lenguaje) throws Exception {
        String respuesta = iniciar(email, "{\"lenguaje\":\"" + lenguaje + "\"}").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(respuesta.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    private ResultActions siguiente(String email, long sesion) throws Exception {
        return mvc.perform(post("/api/dojo/sesiones/" + sesion + "/siguiente").with(tokenDeCliente(email, "mobile")));
    }

    private long idDe(ResultActions respuesta) throws Exception {
        String texto = respuesta.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return Long.parseLong(texto.replaceAll(".*?\"id\":(\\d+).*", "$1"));
    }

    private int entregasDe(long sesion) {
        return jdbc.sql("SELECT count(*) FROM app_schema.app_sesion_dojo_ejercicio WHERE sesion_id = ?")
                .param(sesion).query(Integer.class).single();
    }

    // ---------- iniciar la sesión (CA-53.1) ----------

    @Test
    void iniciar_crea_la_sesion_del_estudiante_en_el_lenguaje_pedido() throws Exception {
        String yo = alumnoInscritoEn(CURSO);

        iniciar(yo, "{\"lenguaje\":\"Python\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.lenguaje").value("python"));

        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_sesion_dojo WHERE lenguaje = 'python' AND fecha_fin IS NULL AND estudiante_id = (SELECT id FROM app_schema.app_estudiante WHERE email = ?)")
                .param(yo).query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void un_lenguaje_ausente_o_desconocido_es_un_400_y_no_crea_nada() throws Exception {
        String yo = alumnoInscritoEn(CURSO);

        iniciar(yo, "{}").andExpect(status().isBadRequest());
        iniciar(yo, "{\"lenguaje\":\"\"}").andExpect(status().isBadRequest());
        iniciar(yo, "{\"lenguaje\":\"cobol\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("lenguaje debe ser python, java o javascript."));

        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_sesion_dojo WHERE estudiante_id = (SELECT id FROM app_schema.app_estudiante WHERE email = ?)")
                .param(yo).query(Integer.class).single()).isZero();
    }

    @Test
    void sin_token_no_se_puede_iniciar_ni_pedir_ejercicios() throws Exception {
        mvc.perform(post("/api/dojo/sesiones").contentType(APPLICATION_JSON).content("{\"lenguaje\":\"python\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/dojo/sesiones/1/siguiente")).andExpect(status().isUnauthorized());
    }

    // ---------- el siguiente ejercicio (CA-53.2) ----------

    @Test
    void entrega_un_ejercicio_con_la_forma_del_detalle_y_sin_la_respuesta_esperada() throws Exception {
        String yo = alumnoInscritoEn(CURSO);
        publicar(CURSO, problema(7, "python", "prediccion_salida", CURSO));
        long sesion = iniciarSesion(yo, "python");

        siguiente(yo, sesion)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.curso_id").value(CURSO))
                .andExpect(jsonPath("$.tipo_ejercicio").value("prediccion_salida"))
                .andExpect(jsonPath("$.enunciado").value("¿Qué imprime? 7"))
                .andExpect(jsonPath("$.opciones", hasSize(0)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(RESPUESTA_SECRETA))));
    }

    @Test
    void las_alternativas_de_una_opcion_multiple_llegan_sin_marca_de_cual_es_la_correcta() throws Exception {
        String yo = alumnoInscritoEn(CURSO);
        ProblemaDto opcion = new ProblemaDto(8, "Uno", "¿Cuál?", "python", "facil", "movil", "opcion_multiple", "",
                List.of(), CURSO, List.of(new ProblemaDto.Opcion(81, "Una"), new ProblemaDto.Opcion(82, "Dos")));
        publicar(CURSO, opcion);

        siguiente(yo, iniciarSesion(yo, "python"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opciones", hasSize(2)))
                .andExpect(jsonPath("$.opciones[0].es_correcta").doesNotExist());
    }

    // ---------- sin repetir (CA-53.3) ----------

    @Test
    void entrega_todos_los_ejercicios_una_sola_vez_y_despues_responde_204() throws Exception {
        String yo = alumnoInscritoEn(CURSO);
        publicar(CURSO, problema(1, "python", "prediccion_salida", CURSO),
                problema(2, "python", "opcion_multiple", CURSO), problema(3, "python", "completar_espacio", CURSO));
        long sesion = iniciarSesion(yo, "python");

        Set<Long> entregados = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            assertThat(entregados.add(idDe(siguiente(yo, sesion)))).as("entrega %d repetida", i + 1).isTrue();
        }

        assertThat(entregados).containsExactlyInAnyOrder(1L, 2L, 3L);
        siguiente(yo, sesion).andExpect(status().isNoContent());
        siguiente(yo, sesion).andExpect(status().isNoContent());
        assertThat(entregasDe(sesion)).isEqualTo(3);
    }

    @Test
    void cada_entrega_queda_guardada_con_su_orden() throws Exception {
        String yo = alumnoInscritoEn(CURSO);
        publicar(CURSO, problema(1, "python", "prediccion_salida", CURSO), problema(2, "python", "opcion_multiple", CURSO));
        long sesion = iniciarSesion(yo, "python");

        siguiente(yo, sesion).andExpect(status().isOk());
        siguiente(yo, sesion).andExpect(status().isOk());

        List<Integer> ordenes = jdbc.sql("SELECT orden FROM app_schema.app_sesion_dojo_ejercicio WHERE sesion_id = ? ORDER BY orden")
                .param(sesion).query(Integer.class).list();
        assertThat(ordenes).containsExactly(1, 2);
        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_sesion_dojo_ejercicio WHERE sesion_id = ? AND acerto IS NULL")
                .param(sesion).query(Integer.class).single()).isEqualTo(2);
    }

    // El mismo ejercicio puede estar en la lista de más de un curso, o dos ejercicios en cursos distintos: sin repetir.
    @Test
    void con_ejercicios_en_varios_cursos_no_repite_ninguno() throws Exception {
        String yo = alumnoInscritoEn(CURSO, OTRO_CURSO);
        publicar(CURSO, problema(1, "python", "prediccion_salida", CURSO), problema(2, "python", "opcion_multiple", CURSO));
        publicar(OTRO_CURSO, problema(3, "python", "completar_espacio", OTRO_CURSO));
        long sesion = iniciarSesion(yo, "python");

        Set<Long> entregados = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            entregados.add(idDe(siguiente(yo, sesion)));
        }

        assertThat(entregados).containsExactlyInAnyOrder(1L, 2L, 3L);
        siguiente(yo, sesion).andExpect(status().isNoContent());
    }

    @Test
    void otra_sesion_puede_volver_a_entregar_los_mismos_ejercicios() throws Exception {
        String yo = alumnoInscritoEn(CURSO);
        publicar(CURSO, problema(1, "python", "prediccion_salida", CURSO));
        long primera = iniciarSesion(yo, "python");
        long segunda = iniciarSesion(yo, "python");

        assertThat(idDe(siguiente(yo, primera))).isEqualTo(1);
        assertThat(idDe(siguiente(yo, segunda))).isEqualTo(1);
    }

    // ---------- qué ejercicios son candidatos ----------

    @Test
    void solo_entrega_ejercicios_del_lenguaje_de_la_sesion() throws Exception {
        String yo = alumnoInscritoEn(CURSO);
        publicar(CURSO, problema(1, "java", "prediccion_salida", CURSO), problema(2, "python", "prediccion_salida", CURSO));
        long sesion = iniciarSesion(yo, "python");

        assertThat(idDe(siguiente(yo, sesion))).isEqualTo(2);
        siguiente(yo, sesion).andExpect(status().isNoContent());
    }

    @Test
    void nunca_entrega_un_ejercicio_de_escribir_codigo() throws Exception {
        String yo = alumnoInscritoEn(CURSO);
        publicar(CURSO, problema(1, "python", "escritura_codigo", CURSO));
        long sesion = iniciarSesion(yo, "python");

        siguiente(yo, sesion).andExpect(status().isNoContent());
        verify(admin, never()).problema(1);
    }

    @Test
    void no_entrega_ejercicios_de_un_curso_donde_no_esta_inscrito() throws Exception {
        String yo = alumnoInscritoEn(CURSO);
        publicar(CURSO, problema(1, "python", "prediccion_salida", CURSO));
        publicar(OTRO_CURSO, problema(2, "python", "prediccion_salida", OTRO_CURSO));
        long sesion = iniciarSesion(yo, "python");

        assertThat(idDe(siguiente(yo, sesion))).isEqualTo(1);
        siguiente(yo, sesion).andExpect(status().isNoContent());
        verify(admin, never()).problemasDelCurso(eq(OTRO_CURSO), anyString());
    }

    @Test
    void sin_ejercicios_disponibles_responde_204_y_no_guarda_nada() throws Exception {
        String yo = alumnoInscritoEn(CURSO);
        publicar(CURSO);
        long sesion = iniciarSesion(yo, "python");

        siguiente(yo, sesion).andExpect(status().isNoContent());

        assertThat(entregasDe(sesion)).isZero();
    }

    @Test
    void un_estudiante_sin_cursos_recibe_204() throws Exception {
        String yo = alumnoInscritoEn();
        long sesion = iniciarSesion(yo, "python");

        siguiente(yo, sesion).andExpect(status().isNoContent());
    }

    @Test
    void un_ejercicio_que_dejo_de_ser_visible_se_salta_y_no_se_apunta() throws Exception {
        String yo = alumnoInscritoEn(CURSO);
        publicar(CURSO, problema(1, "python", "prediccion_salida", CURSO), problema(2, "python", "prediccion_salida", CURSO));
        when(admin.problema(1)).thenThrow(new ProblemaNoVisibleException());
        long sesion = iniciarSesion(yo, "python");

        assertThat(idDe(siguiente(yo, sesion))).isEqualTo(2);
        siguiente(yo, sesion).andExpect(status().isNoContent());
        assertThat(entregasDe(sesion)).isEqualTo(1);
    }

    @Test
    void un_curso_que_admin_api_ya_no_tiene_no_rompe_la_sesion() throws Exception {
        String yo = alumnoInscritoEn(CURSO, OTRO_CURSO);
        when(admin.problemasDelCurso(OTRO_CURSO, "movil"))
                .thenThrow(new CursoNoExisteException());
        publicar(CURSO, problema(1, "python", "prediccion_salida", CURSO));
        long sesion = iniciarSesion(yo, "python");

        assertThat(idDe(siguiente(yo, sesion))).isEqualTo(1);
    }

    // ---------- acceso a la sesión ----------

    @Test
    void la_sesion_de_otro_estudiante_o_inexistente_da_404_y_no_consulta_a_admin_api() throws Exception {
        String dueno = alumnoInscritoEn(CURSO);
        String otro = alumnoInscritoEn(CURSO);
        long sesion = iniciarSesion(dueno, "python");

        siguiente(otro, sesion).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Sesión no encontrada."));
        siguiente(otro, 999_999_999L).andExpect(status().isNotFound());

        verify(admin, never()).problemasDelCurso(anyLong(), anyString());
        assertThat(entregasDe(sesion)).isZero();
    }

    @Test
    void una_sesion_terminada_da_409_y_no_entrega_nada() throws Exception {
        String yo = alumnoInscritoEn(CURSO);
        publicar(CURSO, problema(1, "python", "prediccion_salida", CURSO));
        long sesion = iniciarSesion(yo, "python");
        jdbc.sql("UPDATE app_schema.app_sesion_dojo SET fecha_fin = now() WHERE id = ?").param(sesion).update();

        siguiente(yo, sesion).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esta sesión ya terminó."));

        assertThat(entregasDe(sesion)).isZero();
    }

    @Test
    void con_admin_api_caido_responde_503_y_no_apunta_nada() throws Exception {
        String yo = alumnoInscritoEn(CURSO);
        when(admin.problemasDelCurso(CURSO, "movil")).thenThrow(new AdminNoDisponibleException("sin conexión", null));
        long sesion = iniciarSesion(yo, "python");

        siguiente(yo, sesion).andExpect(status().isServiceUnavailable());

        assertThat(entregasDe(sesion)).isZero();
    }

    @Test
    void si_admin_api_cae_al_pedir_el_detalle_responde_503_y_no_apunta_la_entrega() throws Exception {
        String yo = alumnoInscritoEn(CURSO);
        publicar(CURSO, problema(1, "python", "prediccion_salida", CURSO));
        when(admin.problema(1)).thenThrow(new AdminNoDisponibleException("sin conexión", null));
        long sesion = iniciarSesion(yo, "python");

        siguiente(yo, sesion).andExpect(status().isServiceUnavailable());

        assertThat(entregasDe(sesion)).isZero();
    }
}
