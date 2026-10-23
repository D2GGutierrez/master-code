package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.CursoDto;

// HU27-T1 (CA-27.1): el estudiante ve solo los cursos en los que está inscrito.
class MisCursosApiTest extends PruebaDeApi {

    private static final CursoDto C10 = new CursoDto(10, "Algoritmos I", "python", "ejemplo.edu.pe");
    private static final CursoDto C11 = new CursoDto(11, "Java Básico", "java", "ejemplo.edu.pe");

    @Test
    void devuelve_solo_los_cursos_del_estudiante_autenticado() throws Exception {
        String email = correoUnico();
        long yo = crearEstudiante(email);
        long otro = crearEstudiante(correoUnico());
        inscribir(yo, 10);
        inscribir(yo, 11);
        inscribir(otro, 12);
        when(admin.cursosPorIds(anyCollection())).thenReturn(List.of(C10, C11));

        mvc.perform(get("/api/mis-cursos").with(tokenDe(email, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                // El más reciente primero: ambos comparten fecha dentro de la transacción y desempata el id.
                .andExpect(jsonPath("$[0].curso_id").value(11))
                .andExpect(jsonPath("$[0].nombre").value("Java Básico"))
                .andExpect(jsonPath("$[0].lenguaje").value("java"))
                .andExpect(jsonPath("$[0].institucion").value("ejemplo.edu.pe"))
                .andExpect(jsonPath("$[0].fecha_inscripcion").exists())
                .andExpect(jsonPath("$[1].curso_id").value(10));

        // A admin-api solo se le piden los cursos de este estudiante, nunca el 12 del otro.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> pedidos = ArgumentCaptor.forClass(Collection.class);
        verify(admin).cursosPorIds(pedidos.capture());
        assertThat(pedidos.getValue()).containsExactlyInAnyOrder(10L, 11L);
    }

    @Test
    void sin_inscripciones_devuelve_lista_vacia_sin_llamar_a_admin_api() throws Exception {
        String email = correoUnico();
        crearEstudiante(email);

        mvc.perform(get("/api/mis-cursos").with(tokenDe(email, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verifyNoInteractions(admin);
    }

    @Test
    void un_curso_que_admin_api_ya_no_tiene_no_se_muestra() throws Exception {
        String email = correoUnico();
        long yo = crearEstudiante(email);
        inscribir(yo, 10);
        inscribir(yo, 11);
        when(admin.cursosPorIds(anyCollection())).thenReturn(List.of(C10));

        mvc.perform(get("/api/mis-cursos").with(tokenDe(email, "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].curso_id").value(10));
    }

    @Test
    void si_admin_api_no_responde_da_503() throws Exception {
        String email = correoUnico();
        inscribir(crearEstudiante(email), 10);
        when(admin.cursosPorIds(anyCollection())).thenThrow(new AdminNoDisponibleException("sin conexión", null));

        mvc.perform(get("/api/mis-cursos").with(tokenDe(email, "estudiante")))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void sin_token_da_401_y_con_token_de_docente_403() throws Exception {
        mvc.perform(get("/api/mis-cursos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/mis-cursos").with(tokenDe(correoUnico(), "docente"))).andExpect(status().isForbidden());
    }
}
