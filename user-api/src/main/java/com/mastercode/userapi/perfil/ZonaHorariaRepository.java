package com.mastercode.userapi.perfil;

import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// H6: la zona horaria de cada estudiante (app_estudiante.zona_horaria, V17). SQL directo con JdbcClient, como el resto.
@Repository
public class ZonaHorariaRepository {

    private final JdbcClient jdbc;
    private final ZoneId porDefecto;

    public ZonaHorariaRepository(JdbcClient jdbc,
            @Value("${estudiante.zona-horaria-por-defecto:America/Lima}") String porDefecto) {
        this.jdbc = jdbc;
        this.porDefecto = ZoneId.of(porDefecto);
    }

    // Una zona es válida si es un nombre de región conocido (America/Lima). Se rechazan los desfases fijos («+05:00»,
    // «UTC-5»): no siguen los cambios de hora de la región y el día local quedaría mal.
    public static boolean esValida(String zona) {
        if (zona == null || zona.isBlank() || zona.length() > 50) {
            return false;
        }
        boolean esRegion = zona.contains("/") && ZoneId.getAvailableZoneIds().contains(zona);
        return esRegion || "UTC".equals(zona);
    }

    public void guardar(long estudianteId, String zona) {
        jdbc.sql("UPDATE app_schema.app_estudiante SET zona_horaria = ? WHERE id = ?")
                .params(zona, estudianteId)
                .update();
    }

    // La zona guardada o, si la app todavía no la mandó, la de por defecto.
    public ZoneId de(long estudianteId) {
        return jdbc.sql("SELECT zona_horaria FROM app_schema.app_estudiante WHERE id = ?")
                .param(estudianteId)
                .query(String.class)
                .optional()
                .filter(ZonaHorariaRepository::esValida)
                .map(ZoneId::of)
                .orElse(porDefecto);
    }
}
