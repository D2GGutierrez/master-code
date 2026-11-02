package com.mastercode.userapi.envio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

// SQL directo con JdbcClient, igual que InscripcionRepository. Un envío oficial solo se inserta.
@Repository
public class EnvioRepository {

    public record Fila(long id, int numeroIntento, OffsetDateTime fechaEnvio) {
    }

    // Una fila del historial (HU41): lo que la pantalla muestra de cada intento. Se devuelve tal cual como JSON.
    // numeroIntento, resultado y puntaje pueden ser null en un envío que no se calificó.
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Resumen(long id, Integer numeroIntento, OffsetDateTime fechaEnvio, String resultado,
            BigDecimal puntaje) {
    }

    // Con dos envíos simultáneos del mismo estudiante al mismo problema, la base rechaza el segundo número repetido
    // (índice único de V5) y se vuelve a intentar con el siguiente. Tres vueltas sobran: el botón se bloquea mientras evalúa.
    private static final int VUELTAS = 3;

    private final JdbcClient jdbc;

    public EnvioRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // El número de intento se calcula en la misma sentencia que inserta: cuenta los envíos anteriores del estudiante
    // en ese problema y suma uno. Un envío siempre es del estudiante del token, nunca de un id que llegue por el cuerpo.
    public Fila registrar(long estudianteId, long problemaId, String plataforma, String codigo, String resultado,
            BigDecimal puntaje) {
        DuplicateKeyException ultimo = null;
        for (int vuelta = 0; vuelta < VUELTAS; vuelta++) {
            try {
                return jdbc.sql("""
                        INSERT INTO app_schema.app_envio
                            (estudiante_id, problema_id, plataforma, codigo, resultado, puntaje, numero_intento)
                        VALUES (:estudiante, :problema, :plataforma, :codigo, :resultado, :puntaje,
                            (SELECT COUNT(*) + 1 FROM app_schema.app_envio
                             WHERE estudiante_id = :estudiante AND problema_id = :problema))
                        RETURNING id, numero_intento, fecha_envio
                        """)
                        .param("estudiante", estudianteId)
                        .param("problema", problemaId)
                        .param("plataforma", plataforma)
                        .param("codigo", codigo)
                        .param("resultado", resultado)
                        .param("puntaje", puntaje)
                        .query(Fila.class)
                        .single();
            } catch (DuplicateKeyException e) {
                ultimo = e;
            }
        }
        throw ultimo;
    }

    // HU41-T1: los envíos del estudiante en un problema, del más reciente al más antiguo; el id desempata dos envíos
    // con la misma fecha. Filtra por estudiante Y problema con el índice ix_app_envio_estudiante_problema.
    public List<Resumen> delEstudiante(long estudianteId, long problemaId) {
        return jdbc.sql("""
                SELECT id, numero_intento, fecha_envio, resultado, puntaje
                FROM app_schema.app_envio
                WHERE estudiante_id = ? AND problema_id = ?
                ORDER BY fecha_envio DESC, id DESC
                """)
                .params(estudianteId, problemaId)
                .query(Resumen.class)
                .list();
    }
}
