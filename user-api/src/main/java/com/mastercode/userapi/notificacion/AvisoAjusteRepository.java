package com.mastercode.userapi.notificacion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// HU52-T2: los ajustes del aviso diario de racha de cada estudiante (app_aviso_ajuste, V19). Un estudiante sin fila usa
// los valores por defecto del teléfono: avisos activados y 20:00.
@Repository
public class AvisoAjusteRepository {

    // Hora local por defecto del aviso (la misma que trae la app).
    public static final LocalTime HORA_POR_DEFECTO = LocalTime.of(20, 0);

    // ultimoAviso: el día local en que ya se le avisó, o null si nunca.
    public record Candidato(long estudianteId, LocalTime hora, LocalDate ultimoAviso) {
    }

    private final JdbcClient jdbc;

    public AvisoAjusteRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // Guarda lo que el estudiante eligió en su teléfono. hora nula deja la que ya tenía (o la de por defecto). Guardar no
    // borra el último día avisado: cambiar la hora no vuelve a avisar el mismo día.
    public void guardar(long estudianteId, boolean activados, LocalTime hora) {
        if (hora == null) {
            jdbc.sql("""
                    INSERT INTO app_schema.app_aviso_ajuste (estudiante_id, activados) VALUES (:estudiante, :activados)
                    ON CONFLICT (estudiante_id) DO UPDATE SET activados = EXCLUDED.activados
                    """)
                    .param("estudiante", estudianteId).param("activados", activados).update();
            return;
        }
        jdbc.sql("""
                INSERT INTO app_schema.app_aviso_ajuste (estudiante_id, activados, hora)
                VALUES (:estudiante, :activados, :hora)
                ON CONFLICT (estudiante_id) DO UPDATE SET activados = EXCLUDED.activados, hora = EXCLUDED.hora
                """)
                .param("estudiante", estudianteId).param("activados", activados).param("hora", hora).update();
    }

    // Los estudiantes que pueden necesitar el aviso: con algún teléfono registrado, con los avisos activados y con una
    // racha viva hace poco. El filtro de fecha es amplio a propósito (cualquier zona horaria queda dentro): la decisión
    // exacta se toma después con el día local de cada uno.
    public List<Candidato> candidatos(LocalDate desde) {
        return jdbc.sql("""
                SELECT d.estudiante_id, COALESCE(a.hora, :horaPorDefecto) AS hora, a.ultimo_aviso
                FROM (SELECT DISTINCT estudiante_id FROM app_schema.app_dispositivo) d
                LEFT JOIN app_schema.app_aviso_ajuste a ON a.estudiante_id = d.estudiante_id
                WHERE COALESCE(a.activados, TRUE)
                  AND EXISTS (SELECT 1 FROM app_schema.app_racha r
                              WHERE r.estudiante_id = d.estudiante_id AND r.racha_actual > 0
                                AND r.fecha_ultima_actividad >= :desde)
                ORDER BY d.estudiante_id
                """)
                .param("horaPorDefecto", HORA_POR_DEFECTO)
                .param("desde", desde)
                .query(Candidato.class)
                .list();
    }

    // Apunta que ya se le avisó en ese día local.
    public void marcarAviso(long estudianteId, LocalDate dia) {
        jdbc.sql("""
                INSERT INTO app_schema.app_aviso_ajuste (estudiante_id, ultimo_aviso) VALUES (:estudiante, :dia)
                ON CONFLICT (estudiante_id) DO UPDATE SET ultimo_aviso = EXCLUDED.ultimo_aviso
                """)
                .param("estudiante", estudianteId).param("dia", dia).update();
    }
}
