package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.ResultActions;

import com.mastercode.userapi.notificacion.AvisoAjusteRepository;
import com.mastercode.userapi.notificacion.AvisoDeRachaServicio;
import com.mastercode.userapi.notificacion.DispositivoRepository;
import com.mastercode.userapi.notificacion.EnviadorDeAvisos;
import com.mastercode.userapi.notificacion.TareaDeAvisos;
import com.mastercode.userapi.perfil.ZonaHorariaRepository;
import com.mastercode.userapi.racha.RachaRepository;

// HU52-T2 (ADR-15): PUT /api/yo/avisos guarda los ajustes del aviso, y AvisoDeRachaServicio decide a quién avisar. El
// servicio se prueba con un enviador falso que anota lo que recibe: no hay Firebase.
class AvisoRachaApiTest extends PruebaDeApi {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final ZoneId TOKIO = ZoneId.of("Asia/Tokyo");

    @Autowired
    private AvisoAjusteRepository ajustes;
    @Autowired
    private RachaRepository rachas;
    @Autowired
    private ZonaHorariaRepository zonas;
    @Autowired
    private DispositivoRepository dispositivos;
    @Autowired
    private ApplicationContext contexto;

    // Anota cada aviso (token y mensaje) y responde si «salió» o no.
    private static class EnviadorFalso implements EnviadorDeAvisos {
        final List<String> tokens = new ArrayList<>();
        final List<String> mensajes = new ArrayList<>();
        boolean sale = true;

        @Override
        public boolean enviar(String token, String titulo, String mensaje) {
            tokens.add(token);
            mensajes.add(mensaje);
            return sale;
        }
    }

    private final EnviadorFalso enviador = new EnviadorFalso();

    private AvisoDeRachaServicio servicio() {
        return new AvisoDeRachaServicio(ajustes, rachas, zonas, dispositivos, enviador);
    }

    private static Instant en(ZoneId zona, int dia, int hora, int minuto) {
        return LocalDateTime.of(2026, 10, dia, hora, minuto).atZone(zona).toInstant();
    }

    // Un estudiante con teléfono (el token es único por prueba), zona de Lima y una racha de 4 días practicada el 4.
    private record Alumno(long id, String email, String token) {
    }

    private Alumno conRachaDeAyer() {
        String email = correoUnico();
        long id = crearEstudiante(email);
        String token = "token-" + id + "-" + System.nanoTime();
        dispositivos.registrar(id, token);
        zonas.guardar(id, "America/Lima");
        // Cuatro días seguidos hasta el 4 de octubre.
        for (int dia = 1; dia <= 4; dia++) {
            rachas.registrarActividad(id, "python", LocalDate.of(2026, 10, dia));
        }
        return new Alumno(id, email, token);
    }

    private ResultActions guardarAjustes(String email, String cuerpo) throws Exception {
        return mvc.perform(put("/api/yo/avisos").with(tokenDeCliente(email, "mobile")).contentType(APPLICATION_JSON).content(cuerpo));
    }

    private int llamadasA(Alumno alumno) {
        return (int) enviador.tokens.stream().filter(alumno.token()::equals).count();
    }

    private LocalTime horaGuardada(long id) {
        return jdbc.sql("SELECT hora FROM app_schema.app_aviso_ajuste WHERE estudiante_id = ?").param(id)
                .query(LocalTime.class).single();
    }

    // ---------- PUT /api/yo/avisos ----------

    @Test
    void guardar_los_ajustes_responde_204_y_los_deja_en_la_base() throws Exception {
        String email = correoUnico();
        long id = crearEstudiante(email);

        guardarAjustes(email, "{\"activados\":false,\"hora\":\"21:30\"}").andExpect(status().isNoContent());

        assertThat(jdbc.sql("SELECT activados FROM app_schema.app_aviso_ajuste WHERE estudiante_id = ?").param(id)
                .query(Boolean.class).single()).isFalse();
        assertThat(horaGuardada(id)).isEqualTo(LocalTime.of(21, 30));
    }

    @Test
    void repetir_el_mismo_ajuste_no_cambia_nada_y_actualizar_pisa_el_anterior() throws Exception {
        String email = correoUnico();
        long id = crearEstudiante(email);

        guardarAjustes(email, "{\"activados\":true,\"hora\":\"20:00\"}").andExpect(status().isNoContent());
        guardarAjustes(email, "{\"activados\":true,\"hora\":\"20:00\"}").andExpect(status().isNoContent());
        guardarAjustes(email, "{\"activados\":true,\"hora\":\"07:15\"}").andExpect(status().isNoContent());

        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_aviso_ajuste WHERE estudiante_id = ?").param(id)
                .query(Integer.class).single()).isEqualTo(1);
        assertThat(horaGuardada(id)).isEqualTo(LocalTime.of(7, 15));
    }

    @Test
    void sin_hora_se_guarda_activados_y_se_conserva_la_hora_anterior() throws Exception {
        String email = correoUnico();
        long id = crearEstudiante(email);
        guardarAjustes(email, "{\"activados\":true,\"hora\":\"22:00\"}").andExpect(status().isNoContent());

        guardarAjustes(email, "{\"activados\":false}").andExpect(status().isNoContent());

        assertThat(horaGuardada(id)).isEqualTo(LocalTime.of(22, 0));
        assertThat(jdbc.sql("SELECT activados FROM app_schema.app_aviso_ajuste WHERE estudiante_id = ?").param(id)
                .query(Boolean.class).single()).isFalse();
    }

    @Test
    void la_primera_vez_sin_hora_queda_la_de_por_defecto() throws Exception {
        String email = correoUnico();
        long id = crearEstudiante(email);

        guardarAjustes(email, "{\"activados\":true}").andExpect(status().isNoContent());

        assertThat(horaGuardada(id)).isEqualTo(LocalTime.of(20, 0));
    }

    @Test
    void una_hora_mal_escrita_se_rechaza_con_400_y_no_guarda_nada() throws Exception {
        String email = correoUnico();
        long id = crearEstudiante(email);

        for (String hora : List.of("24:00", "8:00", "20:60", "20-00", "ocho", "20:00:00", "")) {
            guardarAjustes(email, "{\"activados\":true,\"hora\":\"" + hora + "\"}").andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").exists());
        }
        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_aviso_ajuste WHERE estudiante_id = ?").param(id)
                .query(Integer.class).single()).isZero();
    }

    @Test
    void falta_activados() throws Exception {
        String email = correoUnico();
        crearEstudiante(email);

        guardarAjustes(email, "{\"hora\":\"20:00\"}").andExpect(status().isBadRequest());
        guardarAjustes(email, "{}").andExpect(status().isBadRequest());
    }

    @Test
    void sin_token_de_sesion_responde_401() throws Exception {
        mvc.perform(put("/api/yo/avisos").contentType(APPLICATION_JSON).content("{\"activados\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void los_ajustes_son_los_del_estudiante_del_token_y_no_los_de_otro() throws Exception {
        String yo = correoUnico();
        String otro = correoUnico();
        long idYo = crearEstudiante(yo);
        long idOtro = crearEstudiante(otro);

        guardarAjustes(yo, "{\"activados\":false,\"hora\":\"18:00\"}").andExpect(status().isNoContent());

        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_aviso_ajuste WHERE estudiante_id = ?").param(idOtro)
                .query(Integer.class).single()).isZero();
        assertThat(horaGuardada(idYo)).isEqualTo(LocalTime.of(18, 0));
    }

    // ---------- la tarea ----------

    @Test
    void la_tarea_programada_esta_apagada_por_defecto() {
        assertThat(contexto.getBeansOfType(TareaDeAvisos.class)).isEmpty();
    }

    // ---------- a quién se avisa ----------

    @Test
    void se_avisa_a_quien_practico_ayer_y_no_hoy_pasada_la_hora_con_la_racha_en_el_mensaje() {
        Alumno alumno = conRachaDeAyer();

        servicio().avisarLosQueCorresponde(en(LIMA, 5, 20, 30));

        assertThat(llamadasA(alumno)).isEqualTo(1);
        assertThat(enviador.mensajes.get(enviador.tokens.indexOf(alumno.token()))).contains("4 días seguidos");
    }

    @Test
    void no_se_avisa_antes_de_la_hora() {
        Alumno alumno = conRachaDeAyer();

        servicio().avisarLosQueCorresponde(en(LIMA, 5, 19, 59));

        assertThat(llamadasA(alumno)).isZero();
    }

    @Test
    void se_respeta_la_hora_que_el_estudiante_eligio() throws Exception {
        Alumno alumno = conRachaDeAyer();
        guardarAjustes(alumno.email(), "{\"activados\":true,\"hora\":\"21:00\"}").andExpect(status().isNoContent());

        servicio().avisarLosQueCorresponde(en(LIMA, 5, 20, 30));
        assertThat(llamadasA(alumno)).isZero();

        servicio().avisarLosQueCorresponde(en(LIMA, 5, 21, 0));
        assertThat(llamadasA(alumno)).isEqualTo(1);
    }

    @Test
    void con_los_avisos_desactivados_no_se_avisa() throws Exception {
        Alumno alumno = conRachaDeAyer();
        guardarAjustes(alumno.email(), "{\"activados\":false}").andExpect(status().isNoContent());

        servicio().avisarLosQueCorresponde(en(LIMA, 5, 22, 0));

        assertThat(llamadasA(alumno)).isZero();
    }

    @Test
    void no_se_avisa_a_quien_ya_practico_hoy() {
        Alumno alumno = conRachaDeAyer();
        rachas.registrarActividad(alumno.id(), "python", LocalDate.of(2026, 10, 5));

        servicio().avisarLosQueCorresponde(en(LIMA, 5, 22, 0));

        assertThat(llamadasA(alumno)).isZero();
    }

    @Test
    void no_se_avisa_si_la_racha_ya_se_perdio() {
        Alumno alumno = conRachaDeAyer();

        // El 7 de octubre la última práctica (el 4) ya quedó a tres días: no hay racha que salvar.
        servicio().avisarLosQueCorresponde(en(LIMA, 7, 22, 0));

        assertThat(llamadasA(alumno)).isZero();
    }

    @Test
    void no_se_avisa_a_quien_no_tiene_telefono_registrado() {
        String email = correoUnico();
        long id = crearEstudiante(email);
        zonas.guardar(id, "America/Lima");
        rachas.registrarActividad(id, "python", LocalDate.of(2026, 10, 4));

        servicio().avisarLosQueCorresponde(en(LIMA, 5, 22, 0));

        assertThat(ajustes.candidatos(LocalDate.of(2026, 10, 1)).stream().anyMatch(c -> c.estudianteId() == id)).isFalse();
    }

    @Test
    void se_avisa_una_sola_vez_por_dia_aunque_la_tarea_corra_muchas_veces() {
        Alumno alumno = conRachaDeAyer();

        servicio().avisarLosQueCorresponde(en(LIMA, 5, 20, 0));
        servicio().avisarLosQueCorresponde(en(LIMA, 5, 20, 15));
        servicio().avisarLosQueCorresponde(en(LIMA, 5, 23, 45));

        assertThat(llamadasA(alumno)).isEqualTo(1);
    }

    @Test
    void cambiar_los_ajustes_no_vuelve_a_avisar_el_mismo_dia() throws Exception {
        Alumno alumno = conRachaDeAyer();
        servicio().avisarLosQueCorresponde(en(LIMA, 5, 20, 0));

        guardarAjustes(alumno.email(), "{\"activados\":true,\"hora\":\"20:00\"}").andExpect(status().isNoContent());
        servicio().avisarLosQueCorresponde(en(LIMA, 5, 20, 30));

        assertThat(llamadasA(alumno)).isEqualTo(1);
    }

    @Test
    void al_dia_siguiente_se_vuelve_a_avisar_si_sigue_en_peligro() {
        Alumno alumno = conRachaDeAyer();
        rachas.registrarActividad(alumno.id(), "python", LocalDate.of(2026, 10, 5));

        servicio().avisarLosQueCorresponde(en(LIMA, 5, 21, 0));
        assertThat(llamadasA(alumno)).isZero();

        // El 6 no practicó: el día 5 fue el último, así que esa racha está en peligro de nuevo.
        servicio().avisarLosQueCorresponde(en(LIMA, 6, 21, 0));
        assertThat(llamadasA(alumno)).isEqualTo(1);
    }

    @Test
    void si_el_envio_falla_no_se_marca_el_dia_y_se_reintenta() {
        Alumno alumno = conRachaDeAyer();
        enviador.sale = false;

        servicio().avisarLosQueCorresponde(en(LIMA, 5, 20, 0));
        enviador.sale = true;
        servicio().avisarLosQueCorresponde(en(LIMA, 5, 20, 15));

        assertThat(llamadasA(alumno)).isEqualTo(2);
        servicio().avisarLosQueCorresponde(en(LIMA, 5, 20, 30));
        assertThat(llamadasA(alumno)).isEqualTo(2);
    }

    @Test
    void se_avisa_en_la_hora_local_de_cada_estudiante() {
        Alumno enLima = conRachaDeAyer();
        Alumno enTokio = conRachaDeAyer();
        zonas.guardar(enTokio.id(), "Asia/Tokyo");
        // El día 4 de Tokio ya es otro día respecto del de Lima: se le da una racha de ayer en su propio calendario.
        rachas.registrarActividad(enTokio.id(), "python", LocalDate.of(2026, 10, 5));

        // 01:30 UTC del 6: en Lima son las 20:30 del 5 (racha del 4: en peligro); en Tokio, las 10:30 del 6 (antes de las 20:00).
        servicio().avisarLosQueCorresponde(Instant.parse("2026-10-06T01:30:00Z"));

        assertThat(llamadasA(enLima)).isEqualTo(1);
        assertThat(llamadasA(enTokio)).isZero();

        // Dos horas y media más tarde ya son las 20:00 del 6 en Tokio y su última práctica fue el 5.
        servicio().avisarLosQueCorresponde(Instant.parse("2026-10-06T11:00:00Z"));
        assertThat(llamadasA(enTokio)).isEqualTo(1);
    }

    @Test
    void con_varios_telefonos_el_aviso_llega_a_todos_y_el_dia_se_marca_una_vez() {
        Alumno alumno = conRachaDeAyer();
        String segundo = alumno.token() + "-b";
        dispositivos.registrar(alumno.id(), segundo);

        servicio().avisarLosQueCorresponde(en(LIMA, 5, 20, 0));

        assertThat(enviador.tokens).contains(alumno.token(), segundo);
        servicio().avisarLosQueCorresponde(en(LIMA, 5, 20, 10));
        assertThat(enviador.tokens.stream().filter(segundo::equals).count()).isEqualTo(1);
    }

    @Test
    void el_enviador_de_hoy_no_envia_nada_y_no_marca_el_dia() {
        Alumno alumno = conRachaDeAyer();
        EnviadorDeAvisos real = contexto.getBean(EnviadorDeAvisos.class);

        int avisados = new AvisoDeRachaServicio(ajustes, rachas, zonas, dispositivos, real)
                .avisarLosQueCorresponde(en(LIMA, 5, 20, 30));

        assertThat(avisados).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM app_schema.app_aviso_ajuste WHERE estudiante_id = ? AND ultimo_aviso IS NOT NULL")
                .param(alumno.id()).query(Integer.class).single()).isZero();
    }
}
