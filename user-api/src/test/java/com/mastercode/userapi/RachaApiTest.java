package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.mastercode.userapi.perfil.ZonaHorariaRepository;
import com.mastercode.userapi.racha.RachaRepository;
import com.mastercode.userapi.racha.RachaServicio;

// HU49-T2 y HU49-T4 (ADR-13, D23): la racha cuenta días seguidos con acierto, por lenguaje, con el día del estudiante.
// Las pruebas del servicio eligen el instante de cada acierto; las de la ruta usan el reloj real.
class RachaApiTest extends EjerciciosMovilesBase {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final ZoneId TOKIO = ZoneId.of("Asia/Tokyo");

    @Autowired
    private RachaServicio rachas;

    @Autowired
    private RachaRepository repositorio;

    @Autowired
    private ZonaHorariaRepository zonas;

    // La hora en la zona dada, como instante.
    private static Instant a(ZoneId zona, int dia, int hora, int minuto) {
        return LocalDateTime.of(2026, 10, dia, hora, minuto).atZone(zona).toInstant();
    }

    private RachaServicio.RachaDelDia de(long estudiante, String lenguaje, Instant visto) {
        return rachas.delDia(estudiante, visto).stream().filter(r -> r.lenguaje().equals(lenguaje)).findFirst().orElseThrow();
    }

    private long nuevo() {
        return crearEstudiante(correoUnico());
    }

    // ---------- el servicio, con el reloj elegido ----------

    @Test
    void el_primer_acierto_empieza_la_racha_en_uno() {
        long yo = nuevo();

        rachas.registrarAcierto(yo, "python", a(LIMA, 5, 10, 0));

        RachaServicio.RachaDelDia dia = de(yo, "python", a(LIMA, 5, 11, 0));
        assertThat(dia.rachaActual()).isEqualTo(1);
        assertThat(dia.rachaMaxima()).isEqualTo(1);
        assertThat(dia.practicoHoy()).isTrue();
    }

    @Test
    void varios_aciertos_el_mismo_dia_cuentan_como_un_solo_dia() {
        long yo = nuevo();

        rachas.registrarAcierto(yo, "python", a(LIMA, 5, 8, 0));
        rachas.registrarAcierto(yo, "python", a(LIMA, 5, 12, 0));
        rachas.registrarAcierto(yo, "python", a(LIMA, 5, 22, 0));

        assertThat(de(yo, "python", a(LIMA, 5, 23, 0)).rachaActual()).isEqualTo(1);
    }

    @Test
    void un_acierto_cada_dia_va_sumando() {
        long yo = nuevo();

        rachas.registrarAcierto(yo, "python", a(LIMA, 5, 9, 0));
        rachas.registrarAcierto(yo, "python", a(LIMA, 6, 9, 0));
        rachas.registrarAcierto(yo, "python", a(LIMA, 7, 9, 0));

        RachaServicio.RachaDelDia dia = de(yo, "python", a(LIMA, 7, 10, 0));
        assertThat(dia.rachaActual()).isEqualTo(3);
        assertThat(dia.rachaMaxima()).isEqualTo(3);
    }

    // CA-49.4: saltarse un día corta la racha.
    @Test
    void saltarse_un_dia_corta_la_racha_y_el_siguiente_acierto_empieza_de_nuevo() {
        long yo = nuevo();
        rachas.registrarAcierto(yo, "python", a(LIMA, 5, 9, 0));
        rachas.registrarAcierto(yo, "python", a(LIMA, 6, 9, 0));
        rachas.registrarAcierto(yo, "python", a(LIMA, 7, 9, 0));

        // El 8 no practicó. El 9 todavía no ha practicado: la racha ya se ve cortada.
        assertThat(de(yo, "python", a(LIMA, 8, 20, 0)).rachaActual()).isEqualTo(3);
        assertThat(de(yo, "python", a(LIMA, 9, 8, 0)).rachaActual()).isZero();

        rachas.registrarAcierto(yo, "python", a(LIMA, 9, 9, 0));

        RachaServicio.RachaDelDia dia = de(yo, "python", a(LIMA, 9, 10, 0));
        assertThat(dia.rachaActual()).isEqualTo(1);
        assertThat(dia.rachaMaxima()).isEqualTo(3);
    }

    @Test
    void ayer_sin_practicar_hoy_todavia_no_corta_la_racha() {
        long yo = nuevo();
        rachas.registrarAcierto(yo, "python", a(LIMA, 5, 9, 0));
        rachas.registrarAcierto(yo, "python", a(LIMA, 6, 9, 0));

        RachaServicio.RachaDelDia dia = de(yo, "python", a(LIMA, 7, 23, 59));

        assertThat(dia.rachaActual()).isEqualTo(2);
        assertThat(dia.practicoHoy()).isFalse();
    }

    // CA-49.2: el día es el del estudiante. 23:30 en Lima ya es el día siguiente en UTC.
    @Test
    void el_dia_es_el_de_lima_y_no_el_de_utc() {
        long yo = nuevo();

        // Las dos caen el 6 de octubre en UTC, pero son el 5 y el 6 en Lima: dos días seguidos.
        rachas.registrarAcierto(yo, "python", a(LIMA, 5, 23, 30));
        rachas.registrarAcierto(yo, "python", a(LIMA, 6, 0, 30));

        assertThat(de(yo, "python", a(LIMA, 6, 1, 0)).rachaActual()).isEqualTo(2);
    }

    @Test
    void dos_aciertos_de_distinto_dia_en_utc_pero_el_mismo_dia_en_lima_no_suman() {
        long yo = nuevo();

        // 10:00 y 22:00 del 5 en Lima: el segundo ya es el 6 en UTC.
        rachas.registrarAcierto(yo, "python", a(LIMA, 5, 10, 0));
        rachas.registrarAcierto(yo, "python", a(LIMA, 5, 22, 0));

        assertThat(de(yo, "python", a(LIMA, 5, 23, 0)).rachaActual()).isEqualTo(1);
    }

    @Test
    void la_zona_guardada_decide_donde_empieza_el_dia() {
        long enLima = nuevo();
        long enTokio = nuevo();
        zonas.guardar(enTokio, "Asia/Tokyo");
        // 10:00Z y 20:00Z del 5: en Lima son las 05:00 y las 15:00 del 5; en Tokio, las 19:00 del 5 y las 05:00 del 6.
        Instant temprano = Instant.parse("2026-10-05T10:00:00Z");
        Instant tarde = Instant.parse("2026-10-05T20:00:00Z");

        for (long id : new long[] { enLima, enTokio }) {
            rachas.registrarAcierto(id, "python", temprano);
            rachas.registrarAcierto(id, "python", tarde);
        }

        assertThat(de(enLima, "python", tarde).rachaActual()).isEqualTo(1);
        assertThat(de(enTokio, "python", tarde).rachaActual()).isEqualTo(2);
        assertThat(a(TOKIO, 6, 5, 0)).isEqualTo(tarde);
    }

    // Viajar hacia el oeste hace retroceder el día local: la racha no se pierde ni se suma de más.
    @Test
    void si_el_dia_local_retrocede_por_cambiar_de_zona_la_racha_no_se_pierde() {
        long yo = nuevo();
        zonas.guardar(yo, "Asia/Tokyo");
        rachas.registrarAcierto(yo, "python", a(TOKIO, 5, 9, 0));
        rachas.registrarAcierto(yo, "python", a(TOKIO, 6, 9, 0));
        // Ahora está en Lima: las 10:00 del 6 en Tokio son las 20:00 del 5 en Lima.
        zonas.guardar(yo, "America/Lima");

        rachas.registrarAcierto(yo, "python", a(TOKIO, 6, 10, 0));

        assertThat(de(yo, "python", a(TOKIO, 6, 10, 0)).rachaActual()).isEqualTo(2);
        assertThat(repositorio.listar(yo).get(0).fechaUltimaActividad()).isEqualTo(LocalDate.of(2026, 10, 6));
    }

    @Test
    void la_racha_maxima_se_conserva_aunque_la_actual_se_corte() {
        long yo = nuevo();
        for (int dia = 1; dia <= 5; dia++) {
            rachas.registrarAcierto(yo, "python", a(LIMA, dia, 9, 0));
        }
        rachas.registrarAcierto(yo, "python", a(LIMA, 8, 9, 0));
        rachas.registrarAcierto(yo, "python", a(LIMA, 9, 9, 0));

        RachaServicio.RachaDelDia dia = de(yo, "python", a(LIMA, 9, 10, 0));
        assertThat(dia.rachaActual()).isEqualTo(2);
        assertThat(dia.rachaMaxima()).isEqualTo(5);
    }

    @Test
    void cada_lenguaje_lleva_su_propia_racha() {
        long yo = nuevo();
        rachas.registrarAcierto(yo, "python", a(LIMA, 5, 9, 0));
        rachas.registrarAcierto(yo, "python", a(LIMA, 6, 9, 0));
        rachas.registrarAcierto(yo, "java", a(LIMA, 6, 10, 0));

        assertThat(de(yo, "python", a(LIMA, 6, 11, 0)).rachaActual()).isEqualTo(2);
        assertThat(de(yo, "java", a(LIMA, 6, 11, 0)).rachaActual()).isEqualTo(1);
    }

    @Test
    void cada_estudiante_lleva_la_suya() {
        long uno = nuevo();
        long otro = nuevo();

        rachas.registrarAcierto(uno, "python", a(LIMA, 5, 9, 0));
        rachas.registrarAcierto(uno, "python", a(LIMA, 6, 9, 0));

        assertThat(de(uno, "python", a(LIMA, 6, 10, 0)).rachaActual()).isEqualTo(2);
        assertThat(rachas.delDia(otro, a(LIMA, 6, 10, 0))).isEmpty();
    }

    @Test
    void el_lenguaje_se_normaliza_y_uno_vacio_no_cuenta() {
        long yo = nuevo();

        rachas.registrarAcierto(yo, "  Python ", a(LIMA, 5, 9, 0));
        rachas.registrarAcierto(yo, " ", a(LIMA, 5, 9, 0));
        rachas.registrarAcierto(yo, null, a(LIMA, 5, 9, 0));

        assertThat(rachas.delDia(yo, a(LIMA, 5, 10, 0))).extracting(RachaServicio.RachaDelDia::lenguaje)
                .containsExactly("python");
    }

    // ---------- la ruta, con el reloj real ----------

    private ResultActions consultar(String email) throws Exception {
        return mvc.perform(get("/api/yo/racha").with(tokenDeCliente(email, "mobile")));
    }

    @Test
    void sin_ningun_acierto_la_ruta_da_cero_y_lista_vacia() throws Exception {
        consultar(correoUnico()).andExpect(status().isOk())
                .andExpect(jsonPath("$.racha_actual").value(0))
                .andExpect(jsonPath("$.practico_hoy").value(false))
                .andExpect(jsonPath("$.por_lenguaje", hasSize(0)));
    }

    @Test
    void un_acierto_desde_el_celular_cuenta_para_la_racha_de_hoy() throws Exception {
        publicar(prediccion(1, "facil", "bucles", "42"));
        String email = alumno();

        responder(email, 1, "42").andExpect(status().isOk());

        consultar(email).andExpect(status().isOk())
                .andExpect(jsonPath("$.racha_actual").value(1))
                .andExpect(jsonPath("$.practico_hoy").value(true))
                .andExpect(jsonPath("$.por_lenguaje", hasSize(1)))
                .andExpect(jsonPath("$.por_lenguaje[0].lenguaje").value("python"))
                .andExpect(jsonPath("$.por_lenguaje[0].racha_actual").value(1))
                .andExpect(jsonPath("$.por_lenguaje[0].racha_maxima").value(1))
                .andExpect(jsonPath("$.por_lenguaje[0].practico_hoy").value(true));
    }

    // HU49-T4: un intento fallido no cuenta como actividad.
    @Test
    void una_respuesta_equivocada_no_cuenta_como_actividad() throws Exception {
        publicar(prediccion(1, "facil", "bucles", "42"));
        String email = alumno();

        responder(email, 1, "otra cosa").andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("incorrecto"));

        consultar(email).andExpect(status().isOk())
                .andExpect(jsonPath("$.racha_actual").value(0))
                .andExpect(jsonPath("$.practico_hoy").value(false))
                .andExpect(jsonPath("$.por_lenguaje", hasSize(0)));
    }

    @Test
    void varios_aciertos_hoy_siguen_siendo_un_dia() throws Exception {
        publicar(prediccion(1, "facil", "bucles", "42"), completar(2, "facil", "bucles", "x"));
        String email = alumno();

        responder(email, 1, "42").andExpect(status().isOk());
        responder(email, 2, "x").andExpect(status().isOk());

        consultar(email).andExpect(jsonPath("$.racha_actual").value(1));
    }

    @Test
    void la_ruta_muestra_la_racha_mas_alta_entre_los_lenguajes_y_corta_la_que_se_dejo() throws Exception {
        String email = correoUnico();
        long yo = crearEstudiante(email);
        LocalDate hoy = LocalDate.now(LIMA);
        sembrar(yo, "python", 3, 3, hoy.minusDays(1));
        sembrar(yo, "java", 5, 9, hoy.minusDays(3));
        sembrar(yo, "javascript", 2, 2, hoy);

        consultar(email).andExpect(status().isOk())
                .andExpect(jsonPath("$.racha_actual").value(3))
                .andExpect(jsonPath("$.practico_hoy").value(true))
                .andExpect(jsonPath("$.por_lenguaje", hasSize(3)))
                // Ordenados por lenguaje: java (cortada, pero con su máxima), javascript y python.
                .andExpect(jsonPath("$.por_lenguaje[0].lenguaje").value("java"))
                .andExpect(jsonPath("$.por_lenguaje[0].racha_actual").value(0))
                .andExpect(jsonPath("$.por_lenguaje[0].racha_maxima").value(9))
                .andExpect(jsonPath("$.por_lenguaje[1].lenguaje").value("javascript"))
                .andExpect(jsonPath("$.por_lenguaje[1].practico_hoy").value(true))
                .andExpect(jsonPath("$.por_lenguaje[2].lenguaje").value("python"))
                .andExpect(jsonPath("$.por_lenguaje[2].racha_actual").value(3))
                .andExpect(jsonPath("$.por_lenguaje[2].practico_hoy").value(false));
    }

    @Test
    void la_ruta_solo_muestra_la_racha_del_estudiante_de_la_sesion() throws Exception {
        long otro = crearEstudiante(correoUnico());
        sembrar(otro, "python", 7, 7, LocalDate.now(LIMA));

        consultar(correoUnico()).andExpect(jsonPath("$.racha_actual").value(0));
    }

    @Test
    void sin_sesion_o_sin_ser_estudiante_no_hay_acceso() throws Exception {
        mvc.perform(get("/api/yo/racha")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/yo/racha").with(tokenDe(correoUnico(), "docente"))).andExpect(status().isForbidden());
    }

    @Test
    void la_lista_por_lenguaje_viene_ordenada() {
        long yo = nuevo();
        rachas.registrarAcierto(yo, "python", a(LIMA, 5, 9, 0));
        rachas.registrarAcierto(yo, "java", a(LIMA, 5, 9, 0));

        assertThat(rachas.delDia(yo, a(LIMA, 5, 10, 0))).extracting(RachaServicio.RachaDelDia::lenguaje)
                .isEqualTo(List.of("java", "python"));
    }

    private void sembrar(long estudiante, String lenguaje, int actual, int maxima, LocalDate ultimo) {
        jdbc.sql("""
                INSERT INTO app_schema.app_racha (estudiante_id, lenguaje, racha_actual, racha_maxima, fecha_ultima_actividad)
                VALUES (?, ?, ?, ?, ?)
                """).params(estudiante, lenguaje, actual, maxima, ultimo).update();
    }
}
