package com.mastercode.userapi.avance;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

// Solo lectura con SQL directo: no hace falta una entidad Envio para contar.
@Repository
public class AvanceRepository {

    // JdbcClient llena el record por nombre de columna (problema_id -> problemaId).
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ProblemaAvance(long problemaId, long envios, long desdeWeb, long desdeMovil,
            OffsetDateTime ultimoEnvio) {
    }

    // Una sola consulta agrupada: nunca una por problema.
    // Mide intentos, no resueltos. Qué problemas están resueltos lo calcula EstadoProblemasRepository (HU30-T2).
    private static final String POR_PROBLEMA = """
            SELECT problema_id,
                   COUNT(*) AS envios,
                   COUNT(*) FILTER (WHERE plataforma = 'web') AS desde_web,
                   COUNT(*) FILTER (WHERE plataforma = 'movil') AS desde_movil,
                   MAX(fecha_envio) AS ultimo_envio
            FROM app_schema.app_envio
            WHERE estudiante_id = ?
            GROUP BY problema_id
            ORDER BY problema_id
            """;

    private final JdbcClient jdbc;

    public AvanceRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<ProblemaAvance> porProblema(long estudianteId) {
        return jdbc.sql(POR_PROBLEMA).param(estudianteId).query(ProblemaAvance.class).list();
    }
}
