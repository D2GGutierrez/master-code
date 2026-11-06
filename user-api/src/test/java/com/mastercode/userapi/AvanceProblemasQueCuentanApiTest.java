package com.mastercode.userapi;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.mastercode.userapi.admin.ProblemaResumenDto;
import com.mastercode.userapi.problema.AvanceDelCursoServicio;

// HU43-T4 (CA-43.5 y CA-43.6): qué problemas cuentan para el avance. Cuenta exactamente la lista que admin-api entrega
// al estudiante (aprobados y no descartados; ADR-8 D9): lo que el docente aprueba entra como pendiente y lo que archiva
// sale, con sus envíos. Aquí admin-api es un mock que cambia su lista entre una consulta y otra; entre ambas se
// descarta la caché, que es lo que hace un envío nuevo (HU43-T3).
class AvanceProblemasQueCuentanApiTest extends PruebaDeApi {

    private static final long CURSO = 10;
    private static final OffsetDateTime FECHA = OffsetDateTime.parse("2026-10-28T15:00:00Z");
    private static final ProblemaResumenDto SUMA = problema(7, "ambas");
    private static final ProblemaResumenDto ORDENA = problema(8, "ambas");
    private static final ProblemaResumenDto PREDICE = problema(9, "movil");

    @Autowired
    private AvanceDelCursoServicio servicio;

    private static ProblemaResumenDto problema(long id, String plataforma) {
        return new ProblemaResumenDto(id, "Problema " + id, "python", "facil", plataforma, "escritura_codigo", FECHA);
    }

    private record Alumno(String email, long id) {
    }

    private Alumno alumnoInscrito() {
        String email = correoUnico();
        long id = crearEstudiante(email);
        inscribir(id, CURSO);
        return new Alumno(email, id);
    }

    private ResultActions avance(Alumno alumno, String consulta) throws Exception {
        return mvc.perform(get("/api/cursos/" + CURSO + "/avance" + consulta).with(tokenDe(alumno.email(), "estudiante")));
    }

    private ResultActions avance(Alumno alumno) throws Exception {
        return avance(alumno, "");
    }

    // CA-43.5: sin problemas que contar no hay nada que dividir: todo en cero, no un error.
    @Test
    void un_curso_donde_el_docente_aun_no_aprobo_nada_da_todo_en_cero_aunque_haya_envios_sueltos() throws Exception {
        Alumno yo = alumnoInscrito();
        registrarEnvio(yo.id(), 7, "web", "correcto");
        when(admin.problemasDelCurso(CURSO, null)).thenReturn(List.of());

        avance(yo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.resueltos").value(0))
                .andExpect(jsonPath("$.intentados").value(0))
                .andExpect(jsonPath("$.pendientes").value(0));
    }

    @Test
    void un_curso_con_problemas_y_ningun_envio_los_deja_todos_pendientes() throws Exception {
        Alumno yo = alumnoInscrito();
        when(admin.problemasDelCurso(CURSO, null)).thenReturn(List.of(SUMA, ORDENA, PREDICE));

        avance(yo)
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.resueltos").value(0))
                .andExpect(jsonPath("$.intentados").value(0))
                .andExpect(jsonPath("$.pendientes").value(3));
    }

    // CA-43.6: terminado = todos resueltos; los intentos fallidos de antes y los envíos de problemas que ya no están
    // en la lista no cambian la cuenta.
    @Test
    void un_curso_terminado_da_el_total_resuelto_aunque_haya_intentos_fallidos_y_envios_de_otros_problemas()
            throws Exception {
        Alumno yo = alumnoInscrito();
        registrarEnvio(yo.id(), 7, "web", "incorrecto");
        registrarEnvio(yo.id(), 7, "web", "correcto");
        registrarEnvio(yo.id(), 8, "web", "parcial");
        registrarEnvio(yo.id(), 8, "web", "correcto");
        registrarEnvio(yo.id(), 99, "web", "incorrecto");
        when(admin.problemasDelCurso(CURSO, null)).thenReturn(List.of(SUMA, ORDENA));

        avance(yo)
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.resueltos").value(2))
                .andExpect(jsonPath("$.intentados").value(0))
                .andExpect(jsonPath("$.pendientes").value(0));
    }

    // Lo que el docente archiva deja de contar, también lo que el estudiante ya había resuelto.
    @Test
    void un_problema_que_el_docente_archiva_sale_del_total_y_de_los_resueltos() throws Exception {
        Alumno yo = alumnoInscrito();
        registrarEnvio(yo.id(), 7, "web", "correcto");
        when(admin.problemasDelCurso(CURSO, null)).thenReturn(List.of(SUMA, ORDENA));
        avance(yo)
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.resueltos").value(1))
                .andExpect(jsonPath("$.pendientes").value(1));

        when(admin.problemasDelCurso(CURSO, null)).thenReturn(List.of(ORDENA));
        servicio.invalidar(yo.id(), CURSO);

        avance(yo)
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.resueltos").value(0))
                .andExpect(jsonPath("$.pendientes").value(1));
    }

    // Y un problema que el docente aprueba después entra como pendiente: un curso terminado deja de estarlo.
    @Test
    void un_problema_recien_aprobado_entra_como_pendiente_y_un_curso_terminado_deja_de_estarlo() throws Exception {
        Alumno yo = alumnoInscrito();
        registrarEnvio(yo.id(), 7, "web", "correcto");
        when(admin.problemasDelCurso(CURSO, null)).thenReturn(List.of(SUMA));
        avance(yo).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.pendientes").value(0));

        when(admin.problemasDelCurso(CURSO, null)).thenReturn(List.of(SUMA, ORDENA));
        servicio.invalidar(yo.id(), CURSO);

        avance(yo)
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.resueltos").value(1))
                .andExpect(jsonPath("$.pendientes").value(1));
    }

    // Un problema solo móvil no está en la lista de quien pregunta desde la web: no cuenta, ni siquiera resuelto.
    @Test
    void un_problema_solo_movil_resuelto_en_el_celular_no_cuenta_para_la_web_pero_si_para_el_celular() throws Exception {
        Alumno yo = alumnoInscrito();
        registrarEnvio(yo.id(), 9, "movil", "correcto");
        when(admin.problemasDelCurso(CURSO, "web")).thenReturn(List.of(SUMA, ORDENA));
        when(admin.problemasDelCurso(CURSO, "movil")).thenReturn(List.of(SUMA, ORDENA, PREDICE));

        avance(yo, "?plataforma=web")
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.resueltos").value(0));
        avance(yo, "?plataforma=movil")
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.resueltos").value(1));
    }

    // Las tres cuentas siempre suman el total: ningún problema cae en dos grupos ni en ninguno.
    @Test
    void resueltos_intentados_y_pendientes_siempre_suman_el_total() throws Exception {
        Alumno yo = alumnoInscrito();
        registrarEnvio(yo.id(), 7, "web", "correcto");
        registrarEnvio(yo.id(), 7, "web", "incorrecto");
        registrarEnvio(yo.id(), 8, "web", null);
        registrarEnvio(yo.id(), 99, "web", "correcto");
        when(admin.problemasDelCurso(CURSO, null)).thenReturn(List.of(SUMA, ORDENA, PREDICE));

        avance(yo)
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.resueltos").value(1))
                .andExpect(jsonPath("$.intentados").value(1))
                .andExpect(jsonPath("$.pendientes").value(1));
    }
}
