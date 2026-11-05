package com.mastercode.userapi.envio;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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
    // numeroIntento, resultado y puntaje pueden ser null en un envío que no se calificó. mensajeError (HU40-T4) es el
    // aviso que se le mostró al estudiante cuando su código falló; null si no hubo ninguno.
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Resumen(long id, Integer numeroIntento, OffsetDateTime fechaEnvio, String resultado,
            BigDecimal puntaje, String mensajeError) {
    }

    // El detalle de un envío (HU42): lo del historial más el código exacto que se mandó. Un envío anterior a V5 o
    // sembrado sin código lo devuelve vacío, no null, para que el editor siempre reciba un texto.
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Detalle(long id, long problemaId, Integer numeroIntento, OffsetDateTime fechaEnvio, String resultado,
            BigDecimal puntaje, String codigo) {
    }

    // HU44: los envíos de todos los estudiantes en un problema, contados. Solo cifras: ni quién envió ni qué código.
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ResumenDeProblema(long problemaId, long intentos, long correctos) {
    }

    // Lo que pasó con un caso al evaluar un envío (HU37). salidaObtenida ya viene recortada.
    public record ResultadoCaso(long casoPruebaId, boolean paso, String salidaObtenida) {
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
    // Los resultados por caso (HU37-T1) se insertan en esa misma sentencia: o quedan el envío y todas sus filas, o
    // ninguno, y si el número de intento choca se repite todo junto.
    // mensajeError es el aviso que se le muestra al estudiante (HU40-T4); null si el envío no tuvo ningún error.
    public Fila registrar(long estudianteId, long problemaId, String plataforma, String codigo, String resultado,
            BigDecimal puntaje, String mensajeError, List<ResultadoCaso> casos) {
        Long[] ids = casos.stream().map(ResultadoCaso::casoPruebaId).toArray(Long[]::new);
        Boolean[] pasos = casos.stream().map(ResultadoCaso::paso).toArray(Boolean[]::new);
        String[] salidas = casos.stream().map(ResultadoCaso::salidaObtenida).toArray(String[]::new);
        DuplicateKeyException ultimo = null;
        for (int vuelta = 0; vuelta < VUELTAS; vuelta++) {
            try {
                return jdbc.sql("""
                        WITH nuevo AS (
                            INSERT INTO app_schema.app_envio
                                (estudiante_id, problema_id, plataforma, codigo, resultado, puntaje, mensaje_error,
                                 numero_intento)
                            VALUES (:estudiante, :problema, :plataforma, :codigo, :resultado, :puntaje, :mensajeError,
                                (SELECT COUNT(*) + 1 FROM app_schema.app_envio
                                 WHERE estudiante_id = :estudiante AND problema_id = :problema))
                            RETURNING id, numero_intento, fecha_envio
                        ), por_caso AS (
                            INSERT INTO app_schema.app_resultado_caso_prueba
                                (envio_id, caso_prueba_id, paso, salida_obtenida)
                            SELECT nuevo.id, c.caso_prueba_id, c.paso, c.salida_obtenida
                            FROM nuevo,
                                unnest(CAST(:ids AS bigint[]), CAST(:pasos AS boolean[]), CAST(:salidas AS text[]))
                                    AS c(caso_prueba_id, paso, salida_obtenida)
                        )
                        SELECT id, numero_intento, fecha_envio FROM nuevo
                        """)
                        .param("estudiante", estudianteId)
                        .param("problema", problemaId)
                        .param("plataforma", plataforma)
                        .param("codigo", codigo)
                        .param("resultado", resultado)
                        .param("puntaje", puntaje)
                        // Tipo explícito: sin mensaje el valor es null y el driver no sabría de qué tipo es.
                        .param("mensajeError", mensajeError, Types.VARCHAR)
                        .param("ids", ids)
                        .param("pasos", pasos)
                        .param("salidas", salidas)
                        .query(Fila.class)
                        .single();
            } catch (DuplicateKeyException e) {
                ultimo = e;
            }
        }
        throw ultimo;
    }

    // HU42-T2: un envío por su id, solo si es del estudiante. Filtra por el id Y por el estudiante en la misma consulta:
    // el envío de otro es indistinguible de uno que no existe.
    public Optional<Detalle> detalle(long estudianteId, long envioId) {
        return jdbc.sql("""
                SELECT id, problema_id, numero_intento, fecha_envio, resultado, puntaje, COALESCE(codigo, '') AS codigo
                FROM app_schema.app_envio
                WHERE id = ? AND estudiante_id = ?
                """)
                .params(envioId, estudianteId)
                .query(Detalle.class)
                .optional();
    }

    // HU44-T1: por cada problema de la lista, cuántos envíos tiene y cuántos fueron correctos. Una sola consulta agrupada
    // para todos los problemas del curso, nunca una por problema. Cuenta los envíos de todos los estudiantes; un
    // problema sin envíos no aparece (quien pregunta lo completa con ceros). Un envío sin calificar es un intento.
    public List<ResumenDeProblema> resumenPorProblema(List<Long> problemaIds) {
        return jdbc.sql("""
                SELECT problema_id,
                       COUNT(*) AS intentos,
                       COUNT(*) FILTER (WHERE resultado = 'correcto') AS correctos
                FROM app_schema.app_envio
                WHERE problema_id = ANY (CAST(:ids AS bigint[]))
                GROUP BY problema_id
                ORDER BY problema_id
                """)
                .param("ids", problemaIds.toArray(Long[]::new))
                .query(ResumenDeProblema.class)
                .list();
    }

    // HU41-T1: los envíos del estudiante en un problema, del más reciente al más antiguo; el id desempata dos envíos
    // con la misma fecha. Filtra por estudiante Y problema con el índice ix_app_envio_estudiante_problema.
    public List<Resumen> delEstudiante(long estudianteId, long problemaId) {
        return jdbc.sql("""
                SELECT id, numero_intento, fecha_envio, resultado, puntaje, mensaje_error
                FROM app_schema.app_envio
                WHERE estudiante_id = ? AND problema_id = ?
                ORDER BY fecha_envio DESC, id DESC
                """)
                .params(estudianteId, problemaId)
                .query(Resumen.class)
                .list();
    }
}
