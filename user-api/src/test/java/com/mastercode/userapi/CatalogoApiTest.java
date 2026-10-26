package com.mastercode.userapi;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.CursoDto;

// HU25-T2 (user-api lista los cursos publicados que entrega admin-api) y HU25-T4 (solo los de la institución del
// claim institucion del token; sin coincidencias, lista vacía para que la pantalla diga «sin resultados»).
class CatalogoApiTest extends PruebaDeApi {

    private static final String MI_INSTITUCION = "ejemplo.edu.pe";
    private static final CursoDto C10 = new CursoDto(10, "Algoritmos I", "python", MI_INSTITUCION);
    private static final CursoDto C11 = new CursoDto(11, "Java Básico", "java", MI_INSTITUCION);
    private static final CursoDto AJENO = new CursoDto(12, "Algoritmos II", "python", "otra.edu.pe");

    @Test
    void lista_los_cursos_publicados_que_entrega_admin_api() throws Exception {
        when(admin.cursosPublicados(MI_INSTITUCION, null)).thenReturn(List.of(C10, C11));

        mvc.perform(get("/api/cursos").with(tokenDe(correoUnico(), "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].curso_id").value(10))
                .andExpect(jsonPath("$[0].nombre").value("Algoritmos I"))
                .andExpect(jsonPath("$[0].lenguaje").value("python"))
                .andExpect(jsonPath("$[0].institucion").value(MI_INSTITUCION))
                .andExpect(jsonPath("$[1].curso_id").value(11))
                // Cuatro campos exactos: nada más sale hacia el estudiante (por ejemplo, ningún código de invitación).
                .andExpect(jsonPath("$[0].*", hasSize(4)));
    }

    @Test
    void la_institucion_la_pone_el_token_y_no_un_parametro() throws Exception {
        when(admin.cursosPublicados(MI_INSTITUCION, null)).thenReturn(List.of(C10));

        mvc.perform(get("/api/cursos?institucion=otra.edu.pe").with(tokenDe(correoUnico(), "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(admin).cursosPublicados(MI_INSTITUCION, null);
    }

    @Test
    void un_estudiante_de_otra_institucion_ve_los_cursos_de_la_suya() throws Exception {
        when(admin.cursosPublicados("otra.edu.pe", null)).thenReturn(List.of(AJENO));

        mvc.perform(get("/api/cursos").with(tokenDe(correoUnico(), "estudiante", "otra.edu.pe")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].curso_id").value(12));
    }

    @Test
    void aunque_admin_api_entregara_cursos_de_otra_institucion_no_se_muestran() throws Exception {
        when(admin.cursosPublicados(MI_INSTITUCION, null)).thenReturn(List.of(C10, AJENO));

        mvc.perform(get("/api/cursos").with(tokenDe(correoUnico(), "estudiante")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].curso_id").value(10));
    }

    @Test
    void la_busqueda_se_pasa_a_admin_api_sin_espacios_sobrantes() throws Exception {
        when(admin.cursosPublicados(MI_INSTITUCION, "algoritmos")).thenReturn(List.of(C10));

        mvc.perform(get("/api/cursos").param("buscar", "  algoritmos ").with(tokenDe(correoUnico(), "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(admin).cursosPublicados(MI_INSTITUCION, "algoritmos");
    }

    @Test
    void una_busqueda_vacia_equivale_a_no_buscar() throws Exception {
        when(admin.cursosPublicados(MI_INSTITUCION, null)).thenReturn(List.of(C10, C11));

        mvc.perform(get("/api/cursos?buscar=").with(tokenDe(correoUnico(), "estudiante")))
                .andExpect(jsonPath("$", hasSize(2)));

        verify(admin).cursosPublicados(MI_INSTITUCION, null);
    }

    @Test
    void sin_coincidencias_responde_lista_vacia() throws Exception {
        when(admin.cursosPublicados(MI_INSTITUCION, "zzz")).thenReturn(List.of());

        mvc.perform(get("/api/cursos?buscar=zzz").with(tokenDe(correoUnico(), "estudiante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void si_admin_api_no_responde_da_503() throws Exception {
        when(admin.cursosPublicados(MI_INSTITUCION, null)).thenThrow(new AdminNoDisponibleException("sin conexión", null));

        mvc.perform(get("/api/cursos").with(tokenDe(correoUnico(), "estudiante")))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void sin_token_da_401_y_con_token_de_docente_403() throws Exception {
        mvc.perform(get("/api/cursos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cursos").with(tokenDe(correoUnico(), "docente"))).andExpect(status().isForbidden());
    }
}
