package com.mastercode.userapi.problema;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// Solo lectura con SQL directo, igual que AvanceRepository.
@Repository
public class EstadoProblemasRepository {

    // JdbcClient llena el record por nombre de columna (problema_id -> problemaId).
    public record Fila(long problemaId, boolean resuelto) {
    }

    private final JdbcClient jdbc;

    public EstadoProblemasRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // Una sola consulta agrupada con todos los problemas en los que el estudiante tiene envíos, nunca una por
    // problema. La clave es el problema; el valor, si algún envío fue correcto. Un problema que no aparece
    // no tiene envíos (EstadoProblema.de). Los envíos de web y de móvil cuentan igual: son de la misma cuenta.
    // Un envío sin resultado (NULL) cuenta como intento: BOOL_OR ignora los NULL y COALESCE cubre el caso en
    // que todos lo sean.
    public Map<Long, Boolean> resueltosPorProblema(long estudianteId) {
        return jdbc.sql("""
                SELECT problema_id,
                       COALESCE(BOOL_OR(resultado = 'correcto'), FALSE) AS resuelto
                FROM app_schema.app_envio
                WHERE estudiante_id = ?
                GROUP BY problema_id
                """)
                .param(estudianteId)
                .query(Fila.class)
                .list()
                .stream()
                .collect(Collectors.toMap(Fila::problemaId, Fila::resuelto));
    }
}
