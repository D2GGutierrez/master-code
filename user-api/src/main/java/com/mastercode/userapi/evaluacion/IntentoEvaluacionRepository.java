package com.mastercode.userapi.evaluacion;

import java.time.OffsetDateTime;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// SQL directo con JdbcClient, igual que BorradorRepository. Un intento por estudiante y evaluación (V9).
@Repository
public class IntentoEvaluacionRepository {

    private final JdbcClient jdbc;

    public IntentoEvaluacionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // Registra que el estudiante empezó (la primera vez) y devuelve cuándo empezó. Volver a entrar no reinicia el
    // reloj: si ya había un intento, devuelve su inicio de siempre. Una sola sentencia, así dos entradas a la vez no
    // chocan con el único (estudiante, evaluación). El DO UPDATE no cambia nada: solo hace que RETURNING devuelva la
    // fila que ya existía. La hora la pone quien llama, la misma con la que luego compara el vencimiento.
    public OffsetDateTime iniciar(long estudianteId, long evaluacionId, OffsetDateTime ahora) {
        return jdbc.sql("""
                INSERT INTO app_schema.app_intento_evaluacion (estudiante_id, evaluacion_id, fecha_inicio)
                VALUES (:estudiante, :evaluacion, :ahora)
                ON CONFLICT (estudiante_id, evaluacion_id)
                DO UPDATE SET estudiante_id = EXCLUDED.estudiante_id
                RETURNING fecha_inicio
                """)
                .param("estudiante", estudianteId)
                .param("evaluacion", evaluacionId)
                .param("ahora", ahora)
                .query(OffsetDateTime.class)
                .single();
    }
}
