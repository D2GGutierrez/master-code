package com.mastercode.userapi.borrador;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

// SQL directo con JdbcClient, igual que EnvioRepository. Hay un solo borrador por estudiante y problema (V6).
@Repository
public class BorradorRepository {

    // Lo que el editor necesita para abrirse con lo último que el estudiante escribió (HU35).
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Borrador(String codigo, OffsetDateTime fechaActualizacion) {
    }

    private final JdbcClient jdbc;

    public BorradorRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // El estudiante es siempre el del token: no hay forma de leer el borrador de otro.
    public Optional<Borrador> deEstudiante(long estudianteId, long problemaId) {
        return jdbc.sql("""
                SELECT codigo, fecha_actualizacion
                FROM app_schema.app_borrador
                WHERE estudiante_id = :estudiante AND problema_id = :problema
                """)
                .param("estudiante", estudianteId)
                .param("problema", problemaId)
                .query(Borrador.class)
                .optional();
    }

    // Crea el borrador o reemplaza el anterior en una sola sentencia: dos guardados seguidos (autoguardado) no chocan
    // con el único (estudiante, problema). Un texto vacío también se guarda: borrar todo el código es un borrador válido.
    public OffsetDateTime guardar(long estudianteId, long problemaId, String codigo) {
        return jdbc.sql("""
                INSERT INTO app_schema.app_borrador (estudiante_id, problema_id, codigo)
                VALUES (:estudiante, :problema, :codigo)
                ON CONFLICT (estudiante_id, problema_id)
                DO UPDATE SET codigo = EXCLUDED.codigo, fecha_actualizacion = now()
                RETURNING fecha_actualizacion
                """)
                .param("estudiante", estudianteId)
                .param("problema", problemaId)
                .param("codigo", codigo)
                .query(OffsetDateTime.class)
                .single();
    }
}
