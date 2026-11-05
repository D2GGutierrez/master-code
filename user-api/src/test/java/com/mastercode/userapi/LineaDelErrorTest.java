package com.mastercode.userapi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.mastercode.userapi.envio.LineaDelError;

// HU40-T2: sacar la línea del texto del motor es una regla pura, así que se prueba solo con textos.
class LineaDelErrorTest {

    @Test
    void entiende_el_formato_de_java() {
        assertThat(LineaDelError.de("Main.java:12: error: ';' expected\n  int x = 1\n ^\n1 error")).hasValue(12);
    }

    @Test
    void entiende_el_formato_de_javascript() {
        assertThat(LineaDelError.de("/piston/jobs/abc/main.js:7\nconsole.log(\n        ^\nSyntaxError: missing ) after argument list"))
                .hasValue(7);
    }

    @Test
    void entiende_el_formato_de_python() {
        assertThat(LineaDelError.de("  File \"/piston/jobs/abc/main.py\", line 4\n    print(\n         ^\nSyntaxError: '(' was never closed"))
                .hasValue(4);
    }

    @Test
    void toma_la_primera_linea_que_aparece() {
        assertThat(LineaDelError.de("Main.java:3: error: x\nMain.java:9: error: y\n2 errors")).hasValue(3);
    }

    @Test
    void sin_linea_en_el_texto_no_devuelve_nada() {
        assertThat(LineaDelError.de("SyntaxError en la línea 2")).isEmpty();
        assertThat(LineaDelError.de("error: compilation failed")).isEmpty();
        assertThat(LineaDelError.de("")).isEmpty();
        assertThat(LineaDelError.de(null)).isEmpty();
    }

    @Test
    void una_linea_cero_o_un_numero_enorme_no_cuentan() {
        assertThat(LineaDelError.de("Main.java:0: error")).isEmpty();
        assertThat(LineaDelError.de("Main.java:99999999999: error")).isEmpty();
    }

    @Test
    void la_palabra_line_dentro_de_otra_no_cuenta() {
        assertThat(LineaDelError.de("online 5 times")).isEmpty();
    }
}
