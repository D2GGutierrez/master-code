package com.mastercode.userapi.cinturon;

import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// SQL directo con JdbcClient, igual que DispositivoRepository. Tablas: app_cinturon, app_nivel_cinturon (V11) y
// app_puntos_ejercicio (V13).
@Repository
public class CinturonRepository {

    // El cinturón de un estudiante en un lenguaje: su nivel (1 = blanco) y los puntos que lleva.
    public record Cinturon(long estudianteId, String lenguaje, int nivel, int puntos) {
    }

    private final JdbcClient jdbc;

    public CinturonRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Cinturon> buscar(long estudianteId, String lenguaje) {
        return jdbc.sql("""
                SELECT estudiante_id, lenguaje, nivel, puntos
                FROM app_schema.app_cinturon
                WHERE estudiante_id = ? AND lenguaje = ?
                """)
                .params(estudianteId, lenguaje)
                .query(Cinturon.class)
                .optional();
    }

    // Apunta que este ejercicio ya dio puntos. true si es la primera vez; false si ya los había dado. Una sola sentencia
    // con ON CONFLICT: dos respuestas correctas a la vez no suman dos veces ni dejan la transacción abortada.
    public boolean registrarPuntosDelEjercicio(long estudianteId, long problemaId, String lenguaje, int puntos) {
        return jdbc.sql("""
                INSERT INTO app_schema.app_puntos_ejercicio (estudiante_id, problema_id, lenguaje, puntos)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (estudiante_id, problema_id) DO NOTHING
                """)
                .params(estudianteId, problemaId, lenguaje, puntos)
                .update() == 1;
    }

    // Suma los puntos al cinturón del lenguaje, lo crea si no existe y deja el nivel que corresponde a los puntos nuevos:
    // el más alto cuyos puntos requeridos ya se alcanzaron (puede subir más de un nivel de golpe).
    public Cinturon sumar(long estudianteId, String lenguaje, int puntos) {
        return jdbc.sql("""
                INSERT INTO app_schema.app_cinturon (estudiante_id, lenguaje, puntos, nivel)
                VALUES (:estudiante, :lenguaje, :puntos,
                        (SELECT max(nivel) FROM app_schema.app_nivel_cinturon WHERE puntos_requeridos <= :puntos))
                ON CONFLICT (estudiante_id, lenguaje) DO UPDATE SET
                    puntos = app_cinturon.puntos + EXCLUDED.puntos,
                    nivel = (SELECT max(nivel) FROM app_schema.app_nivel_cinturon
                             WHERE puntos_requeridos <= app_cinturon.puntos + EXCLUDED.puntos),
                    fecha_actualizacion = now()
                RETURNING estudiante_id, lenguaje, nivel, puntos
                """)
                .param("estudiante", estudianteId)
                .param("lenguaje", lenguaje)
                .param("puntos", puntos)
                .query(Cinturon.class)
                .single();
    }
}
