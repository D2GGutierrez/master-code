package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.mastercode.userapi.seguridad.ClaveInternaFiltro;

// D3: el filtro que protege /internal/. Sin Spring: se prueba la clase sola.
class ClaveInternaFiltroTest {

    private final MockHttpServletResponse respuesta = new MockHttpServletResponse();
    private final MockFilterChain cadena = new MockFilterChain();

    private void filtrar(String clave, String ruta, String cabecera) throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", ruta);
        if (cabecera != null) {
            peticion.addHeader("X-Internal-Key", cabecera);
        }
        new ClaveInternaFiltro(clave).doFilter(peticion, respuesta, cadena);
    }

    // MockFilterChain guarda la petición solo si el filtro dejó pasar.
    private boolean dejoPasar() {
        return cadena.getRequest() != null;
    }

    @Test
    void con_la_clave_correcta_deja_pasar() throws Exception {
        filtrar("secreta", "/internal/cursos/1/inscritos", "secreta");

        assertThat(dejoPasar()).isTrue();
        assertThat(respuesta.getStatus()).isEqualTo(200);
    }

    @Test
    void con_clave_equivocada_o_sin_clave_responde_403() throws Exception {
        filtrar("secreta", "/internal/cursos/1/inscritos", "otra");
        assertThat(dejoPasar()).isFalse();
        assertThat(respuesta.getStatus()).isEqualTo(403);

        MockHttpServletResponse otraRespuesta = new MockHttpServletResponse();
        new ClaveInternaFiltro("secreta").doFilter(
                new MockHttpServletRequest("GET", "/internal/cursos/1/inscritos"), otraRespuesta, new MockFilterChain());
        assertThat(otraRespuesta.getStatus()).isEqualTo(403);
    }

    @Test
    void si_el_servidor_no_tiene_clave_configurada_rechaza_todo_incluso_una_cabecera_vacia() throws Exception {
        filtrar("", "/internal/cursos/1/inscritos", "");

        assertThat(dejoPasar()).isFalse();
        assertThat(respuesta.getStatus()).isEqualTo(403);
    }

    @Test
    void no_toca_las_rutas_que_no_son_internas() throws Exception {
        filtrar("secreta", "/api/yo", null);

        assertThat(dejoPasar()).isTrue();
    }
}
