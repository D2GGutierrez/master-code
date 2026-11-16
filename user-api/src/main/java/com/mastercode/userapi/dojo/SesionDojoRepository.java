package com.mastercode.userapi.dojo;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// SQL directo con JdbcClient, igual que DispositivoRepository. Tablas: app_sesion_dojo y app_sesion_dojo_ejercicio (V12).
@Repository
public class SesionDojoRepository {

    // abierta: la sesión no tiene fecha de fin.
    public record Sesion(long id, long estudianteId, String lenguaje, boolean abierta) {
    }

    private final JdbcClient jdbc;

    public SesionDojoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public long crear(long estudianteId, String lenguaje) {
        return jdbc.sql("""
                INSERT INTO app_schema.app_sesion_dojo (estudiante_id, lenguaje) VALUES (?, ?) RETURNING id
                """)
                .params(estudianteId, lenguaje)
                .query(Long.class)
                .single();
    }

    // Solo la sesión del propio estudiante: la de otro es como si no existiera.
    public Optional<Sesion> buscar(long sesionId, long estudianteId) {
        return jdbc.sql("""
                SELECT id, estudiante_id, lenguaje, fecha_fin IS NULL AS abierta
                FROM app_schema.app_sesion_dojo
                WHERE id = ? AND estudiante_id = ?
                """)
                .params(sesionId, estudianteId)
                .query(Sesion.class)
                .optional();
    }

    // Los problemas que ya se entregaron en la sesión.
    public Set<Long> entregados(long sesionId) {
        return jdbc.sql("SELECT problema_id FROM app_schema.app_sesion_dojo_ejercicio WHERE sesion_id = ?")
                .param(sesionId)
                .query(Long.class)
                .list()
                .stream()
                .collect(Collectors.toSet());
    }

    // Apunta la entrega con el siguiente número de orden. true si quedó guardada; false si ese problema ya estaba entregado
    // o si otra petición a la vez tomó el mismo número de orden (el ON CONFLICT sin destino cubre los dos UNIQUE de V12).
    public boolean registrarEntrega(long sesionId, long problemaId) {
        return jdbc.sql("""
                INSERT INTO app_schema.app_sesion_dojo_ejercicio (sesion_id, problema_id, orden)
                VALUES (:sesion, :problema,
                        (SELECT coalesce(max(orden), 0) + 1 FROM app_schema.app_sesion_dojo_ejercicio WHERE sesion_id = :sesion))
                ON CONFLICT DO NOTHING
                """)
                .param("sesion", sesionId)
                .param("problema", problemaId)
                .update() == 1;
    }
}
