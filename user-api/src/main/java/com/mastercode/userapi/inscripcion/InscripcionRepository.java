package com.mastercode.userapi.inscripcion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// SQL directo con JdbcClient, igual que AvanceRepository: la tabla solo se inserta y se lee.
@Repository
public class InscripcionRepository {

    public record Fila(long id, long cursoId, OffsetDateTime fecha) {
    }

    public record Inscrito(String email, OffsetDateTime fecha) {
    }

    private final JdbcClient jdbc;

    public InscripcionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // ON CONFLICT en vez de "buscar y luego insertar": si el estudiante pulsa dos veces a la vez,
    // la segunda no falla con el UNIQUE (V3) ni deja la transacción abortada; devuelve vacío.
    public Optional<Fila> crear(long estudianteId, long cursoId) {
        return jdbc.sql("""
                INSERT INTO app_schema.app_inscripcion (estudiante_id, curso_id) VALUES (?, ?)
                ON CONFLICT (estudiante_id, curso_id) DO NOTHING
                RETURNING id, curso_id, fecha
                """)
                .params(estudianteId, cursoId)
                .query(Fila.class)
                .optional();
    }

    // Las más recientes primero; el id desempata fechas iguales para que el orden sea estable.
    public List<Fila> deEstudiante(long estudianteId) {
        return jdbc.sql("""
                SELECT id, curso_id, fecha
                FROM app_schema.app_inscripcion
                WHERE estudiante_id = ?
                ORDER BY fecha DESC, id DESC
                """)
                .param(estudianteId)
                .query(Fila.class)
                .list();
    }

    // Los inscritos de un curso, en orden de inscripción (HU28).
    public List<Inscrito> inscritosDeCurso(long cursoId) {
        return jdbc.sql("""
                SELECT e.email, i.fecha
                FROM app_schema.app_inscripcion i
                JOIN app_schema.app_estudiante e ON e.id = i.estudiante_id
                WHERE i.curso_id = ?
                ORDER BY i.fecha, i.id
                """)
                .param(cursoId)
                .query(Inscrito.class)
                .list();
    }

    // ¿Está el estudiante inscrito en el curso? Es la regla de acceso a los problemas (HU29, CA-29.1): se consulta
    // por el estudiante del token y el curso pedido, así la inscripción de otra persona nunca da acceso.
    public boolean estaInscrito(long estudianteId, long cursoId) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM app_schema.app_inscripcion WHERE estudiante_id = ? AND curso_id = ?
                )
                """)
                .params(estudianteId, cursoId)
                .query(Boolean.class)
                .single();
    }
}
