package com.mastercode.userapi.cinturon;

import java.util.List;
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

    // Un nivel de la tabla de referencia: el nombre y los puntos acumulados desde los que se tiene.
    public record Nivel(int nivel, String nombre, int puntosRequeridos) {
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

    // Todos los cinturones del estudiante (uno por lenguaje en el que ya acertó), por lenguaje.
    public List<Cinturon> listar(long estudianteId) {
        return jdbc.sql("""
                SELECT estudiante_id, lenguaje, nivel, puntos
                FROM app_schema.app_cinturon
                WHERE estudiante_id = ?
                ORDER BY lenguaje
                """)
                .param(estudianteId)
                .query(Cinturon.class)
                .list();
    }

    // Los niveles de blanco a negro, de menor a mayor (HU51-T1 calcula con ellos lo que falta para el siguiente).
    public List<Nivel> niveles() {
        return jdbc.sql("""
                SELECT nivel, nombre, puntos_requeridos
                FROM app_schema.app_nivel_cinturon
                ORDER BY nivel
                """)
                .query(Nivel.class)
                .list();
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
