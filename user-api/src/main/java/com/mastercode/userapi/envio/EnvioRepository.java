package com.mastercode.userapi.envio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// SQL directo con JdbcClient, igual que InscripcionRepository. Un envío oficial solo se inserta.
@Repository
public class EnvioRepository {

    public record Fila(long id, int numeroIntento, OffsetDateTime fechaEnvio) {
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
}
