package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.mastercode.userapi.admin.AdminCliente;
import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.CodigoInvalidoException;
import com.mastercode.userapi.admin.CodigoVencidoException;
import com.mastercode.userapi.admin.CursoDto;

// El cliente de la API interna de admin-api (D3). Sin admin-api real: MockRestServiceServer responde.
class AdminClienteTest {

    private static final String CURSO_JSON =
            "{\"id\":10,\"nombre\":\"Algoritmos I\",\"lenguaje\":\"python\",\"institucion\":\"ejemplo.edu.pe\",\"otro\":1}";

    private MockRestServiceServer servidor;
    private AdminCliente cliente;

    @BeforeEach
    void preparar() {
        // Igual que AdminConfig: la clave va como cabecera por defecto en todas las peticiones.
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://admin-api:8000")
                .defaultHeader("X-Internal-Key", "clave-de-prueba");
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new AdminCliente(builder.build());
    }

    @Test
    void por_codigo_pide_la_ruta_interna_con_la_clave_y_lee_el_curso() {
        servidor.expect(requestTo("http://admin-api:8000/internal/cursos/por-codigo/ABCD2345/"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Key", "clave-de-prueba"))
                .andRespond(withSuccess(CURSO_JSON, MediaType.APPLICATION_JSON));

        CursoDto curso = cliente.cursoPorCodigo("ABCD2345");

        // El campo desconocido «otro» se ignora.
        assertThat(curso).isEqualTo(new CursoDto(10, "Algoritmos I", "python", "ejemplo.edu.pe"));
        servidor.verify();
    }

    @Test
    void un_404_es_codigo_invalido() {
        servidor.expect(requestTo("http://admin-api:8000/internal/cursos/por-codigo/ZZZZ9999/"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(CodigoInvalidoException.class, () -> cliente.cursoPorCodigo("ZZZZ9999"));
    }

    @Test
    void un_410_es_codigo_vencido() {
        servidor.expect(requestTo("http://admin-api:8000/internal/cursos/por-codigo/VIEJO234/"))
                .andRespond(withStatus(HttpStatus.GONE));

        assertThrows(CodigoVencidoException.class, () -> cliente.cursoPorCodigo("VIEJO234"));
    }

    @Test
    void un_5xx_un_403_o_la_falta_de_conexion_son_admin_no_disponible() {
        servidor.expect(requestTo("http://admin-api:8000/internal/cursos/por-codigo/ABCD2345/"))
                .andRespond(withServerError());
        assertThrows(AdminNoDisponibleException.class, () -> cliente.cursoPorCodigo("ABCD2345"));

        // 403 = clave interna rechazada: para el estudiante es un fallo del servicio, no un código malo.
        servidor.reset();
        servidor.expect(requestTo("http://admin-api:8000/internal/cursos/por-codigo/ABCD2345/"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        assertThrows(AdminNoDisponibleException.class, () -> cliente.cursoPorCodigo("ABCD2345"));

        servidor.reset();
        servidor.expect(requestTo("http://admin-api:8000/internal/cursos/por-codigo/ABCD2345/"))
                .andRespond(withException(new IOException("sin conexión")));
        assertThrows(AdminNoDisponibleException.class, () -> cliente.cursoPorCodigo("ABCD2345"));
    }

    @Test
    void por_ids_pide_la_lista_y_devuelve_los_cursos() {
        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith("http://admin-api:8000/internal/cursos/?ids=")))
                .andExpect(request -> assertThat(URLDecoder.decode(request.getURI().getRawQuery(), StandardCharsets.UTF_8))
                        .isEqualTo("ids=10,11"))
                .andExpect(header("X-Internal-Key", "clave-de-prueba"))
                .andRespond(withSuccess("[" + CURSO_JSON + "]", MediaType.APPLICATION_JSON));

        List<CursoDto> cursos = cliente.cursosPorIds(List.of(10L, 11L));

        assertThat(cursos).containsExactly(new CursoDto(10, "Algoritmos I", "python", "ejemplo.edu.pe"));
        servidor.verify();
    }

    @Test
    void por_ids_sin_ids_no_hace_ninguna_peticion() {
        assertThat(cliente.cursosPorIds(List.of())).isEmpty();

        servidor.verify();
    }

    @Test
    void por_ids_con_fallo_es_admin_no_disponible() {
        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith("http://admin-api:8000/internal/cursos/")))
                .andRespond(withServerError());

        assertThrows(AdminNoDisponibleException.class, () -> cliente.cursosPorIds(List.of(10L)));
    }
}
