package com.mastercode.userapi.racha;

import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// SQL directo con JdbcClient. Tabla: app_racha (V14), una fila por estudiante y lenguaje.
@Repository
public class RachaRepository {

    // Lo que hay guardado: la racha tal como quedó el último día que el estudiante practicó. Para saber si sigue viva
    // hoy hay que mirar la fecha (RachaServicio).
    public record Racha(String lenguaje, int rachaActual, int rachaMaxima, LocalDate fechaUltimaActividad) {
    }

    private final JdbcClient jdbc;

    public RachaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // HU49-T2 y HU49-T4: apunta un acierto en el día local «hoy». Una sola sentencia, así dos aciertos a la vez no se
    // pisan: el mismo día no cambia la racha, el día siguiente al último la sube en uno y cualquier otro caso (se saltó
    // un día o es el primero) la deja en uno. Si la fecha guardada es posterior a hoy (el estudiante cambió de zona y su
    // día local retrocedió) se trata como el mismo día: la racha no se pierde por viajar.
    public void registrarActividad(long estudianteId, String lenguaje, LocalDate hoy) {
        jdbc.sql("""
                INSERT INTO app_schema.app_racha (estudiante_id, lenguaje, racha_actual, racha_maxima, fecha_ultima_actividad)
                VALUES (:estudiante, :lenguaje, 1, 1, :hoy)
                ON CONFLICT (estudiante_id, lenguaje) DO UPDATE SET
                    racha_actual = CASE
                        WHEN app_racha.fecha_ultima_actividad >= EXCLUDED.fecha_ultima_actividad THEN app_racha.racha_actual
                        WHEN app_racha.fecha_ultima_actividad = EXCLUDED.fecha_ultima_actividad - 1 THEN app_racha.racha_actual + 1
                        ELSE 1 END,
                    racha_maxima = GREATEST(app_racha.racha_maxima, CASE
                        WHEN app_racha.fecha_ultima_actividad >= EXCLUDED.fecha_ultima_actividad THEN app_racha.racha_actual
                        WHEN app_racha.fecha_ultima_actividad = EXCLUDED.fecha_ultima_actividad - 1 THEN app_racha.racha_actual + 1
                        ELSE 1 END),
                    fecha_ultima_actividad = GREATEST(app_racha.fecha_ultima_actividad, EXCLUDED.fecha_ultima_actividad)
                """)
                .param("estudiante", estudianteId)
                .param("lenguaje", lenguaje)
                .param("hoy", hoy)
                .update();
    }

    public List<Racha> listar(long estudianteId) {
        return jdbc.sql("""
                SELECT lenguaje, racha_actual, racha_maxima, fecha_ultima_actividad
                FROM app_schema.app_racha
                WHERE estudiante_id = ?
                ORDER BY lenguaje
                """)
                .param(estudianteId)
                .query(Racha.class)
                .list();
    }
}
