package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import com.mastercode.userapi.cinturon.PuntosPorEjercicio;

// HU50-T4 (CA-50.2): docs/puntos-cinturon.md define los puntos por tipo de ejercicio y los puntos de cada cinturón. Esta
// prueba lee las dos tablas del documento y comprueba que dicen lo mismo que el código (PuntosPorEjercicio) y la base
// (app_nivel_cinturon): si alguien cambia un valor en un solo lugar, falla. Si el documento no está donde se ejecuta
// (por ejemplo, al construir user-api solo), se omite.
@SpringBootTest
@Transactional
class PuntosCinturonDocTest {

    private static final List<Path> RUTAS = List.of(
            Path.of("docs", "puntos-cinturon.md"),
            Path.of("..", "docs", "puntos-cinturon.md"));

    // | `escritura_codigo` | 30 |
    private static final Pattern FILA_DE_PUNTOS = Pattern.compile("^\\|\\s*`([a-z_]+)`\\s*\\|\\s*(\\d+)\\s*\\|\\s*$");
    // | 2 | amarillo | 100 |
    private static final Pattern FILA_DE_NIVEL = Pattern.compile("^\\|\\s*(\\d+)\\s*\\|\\s*([a-z]+)\\s*\\|\\s*(\\d+)\\s*\\|\\s*$");

    @Autowired
    private JdbcClient jdbc;

    private static List<String> lineasDelDocumento() throws IOException {
        for (Path ruta : RUTAS) {
            if (Files.isRegularFile(ruta)) {
                return Files.readAllLines(ruta, StandardCharsets.UTF_8);
            }
        }
        assumeTrue(false, "docs/puntos-cinturon.md no está en esta ruta de ejecución");
        return List.of();
    }

    private static Map<String, Integer> puntosDelDocumento(List<String> lineas) {
        Map<String, Integer> puntos = new LinkedHashMap<>();
        for (String linea : lineas) {
            Matcher m = FILA_DE_PUNTOS.matcher(linea);
            if (m.matches()) {
                puntos.put(m.group(1), Integer.parseInt(m.group(2)));
            }
        }
        return puntos;
    }

    private record NivelDoc(int nivel, String nombre, int puntos) {
    }

    private static List<NivelDoc> nivelesDelDocumento(List<String> lineas) {
        return lineas.stream()
                .map(FILA_DE_NIVEL::matcher)
                .filter(Matcher::matches)
                .map(m -> new NivelDoc(Integer.parseInt(m.group(1)), m.group(2), Integer.parseInt(m.group(3))))
                .toList();
    }

    @Test
    void los_puntos_por_tipo_del_documento_son_los_del_codigo() throws IOException {
        Map<String, Integer> documento = puntosDelDocumento(lineasDelDocumento());

        assertThat(documento).as("tabla de puntos leída del documento").isNotEmpty();
        documento.forEach((tipo, puntos) ->
                assertThat(PuntosPorEjercicio.de(tipo)).as("puntos de %s", tipo).isEqualTo(puntos));
    }

    @Test
    void el_documento_lista_todos_los_tipos_que_dan_puntos() throws IOException {
        Map<String, Integer> documento = puntosDelDocumento(lineasDelDocumento());

        // Si el código da puntos a un tipo nuevo y el documento no lo dice, falla: el documento no puede quedar corto.
        assertThat(documento.keySet()).containsExactlyInAnyOrder(
                "escritura_codigo", "prediccion_salida", "opcion_multiple", "completar_espacio");
    }

    @Test
    void los_puntos_de_cada_tipo_son_los_confirmados() {
        assertThat(PuntosPorEjercicio.de("escritura_codigo")).isEqualTo(30);
        assertThat(PuntosPorEjercicio.de("prediccion_salida")).isEqualTo(10);
        assertThat(PuntosPorEjercicio.de("opcion_multiple")).isEqualTo(10);
        assertThat(PuntosPorEjercicio.de("completar_espacio")).isEqualTo(10);
    }

    @Test
    void los_cinturones_del_documento_son_los_de_la_tabla_de_niveles() throws IOException {
        List<NivelDoc> documento = nivelesDelDocumento(lineasDelDocumento());

        List<NivelDoc> base = jdbc.sql("SELECT nivel, nombre, puntos_requeridos FROM app_schema.app_nivel_cinturon ORDER BY nivel")
                .query((rs, n) -> new NivelDoc(rs.getInt("nivel"), rs.getString("nombre"), rs.getInt("puntos_requeridos")))
                .list();

        assertThat(documento).as("niveles leídos del documento").hasSize(7);
        assertThat(documento).isEqualTo(base);
    }

    @Test
    void los_cinturones_van_de_blanco_a_negro_con_umbrales_crecientes() throws IOException {
        List<NivelDoc> documento = nivelesDelDocumento(lineasDelDocumento());

        assertThat(documento.get(0)).isEqualTo(new NivelDoc(1, "blanco", 0));
        assertThat(documento.get(documento.size() - 1).nombre()).isEqualTo("negro");
        for (int i = 1; i < documento.size(); i++) {
            assertThat(documento.get(i).puntos()).isGreaterThan(documento.get(i - 1).puntos());
        }
    }
}
