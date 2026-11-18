package com.mastercode.userapi.notificacion;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// SQL directo con JdbcClient, igual que BorradorRepository. Un token por fila (V10).
@Repository
public class DispositivoRepository {

    private final JdbcClient jdbc;

    public DispositivoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // Guarda el token del dispositivo del estudiante. Si ya estaba, no se repite: se actualiza el dueño (otro estudiante
    // pudo iniciar sesión en el mismo teléfono) y la fecha. Una sola sentencia, así dos registros a la vez no chocan con
    // el único del token.
    public void registrar(long estudianteId, String token) {
        jdbc.sql("""
                INSERT INTO app_schema.app_dispositivo (estudiante_id, token)
                VALUES (:estudiante, :token)
                ON CONFLICT (token)
                DO UPDATE SET estudiante_id = EXCLUDED.estudiante_id, fecha_registro = now()
                """)
                .param("estudiante", estudianteId)
                .param("token", token)
                .update();
    }

    // HU52-T2: los tokens de todos los teléfonos del estudiante, a donde llegan los avisos.
    public List<String> tokensDe(long estudianteId) {
        return jdbc.sql("SELECT token FROM app_schema.app_dispositivo WHERE estudiante_id = ? ORDER BY id")
                .param(estudianteId)
                .query(String.class)
                .list();
    }
}
