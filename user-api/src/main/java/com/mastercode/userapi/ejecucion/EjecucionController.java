package com.mastercode.userapi.ejecucion;

import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.mastercode.userapi.Detalle;
import com.mastercode.userapi.perfil.Estudiante;
import com.mastercode.userapi.sandbox.EjecucionResultado;
import com.mastercode.userapi.sandbox.EjecucionSolicitud;
import com.mastercode.userapi.sandbox.Estado;
import com.mastercode.userapi.sandbox.LenguajeNoSoportadoException;
import com.mastercode.userapi.sandbox.SandboxCliente;
import com.mastercode.userapi.sandbox.SandboxNoDisponibleException;

// HU06: «Ejecutar» corre el código del estudiante con una entrada de prueba y devuelve lo que pasó.
// A diferencia de un envío (HU36), NO guarda nada: no escribe en app_envio y no cuenta como intento.
@RestController
public class EjecucionController {

    // Mismos lenguajes que admin-api al crear un curso (cursos/serializers.py).
    private static final Set<String> LENGUAJES = Set.of("python", "java", "javascript");
    // Topes que evitan mandar al sandbox cuerpos enormes; ai-service aplica los mismos.
    private static final int MAX_CODIGO = 50_000;
    private static final int MAX_ENTRADA = 10_000;

    record Solicitud(String codigo, String lenguaje, String entrada) {
    }

    private final SandboxCliente sandbox;
    private final LimiteEjecuciones limite;

    public EjecucionController(SandboxCliente sandbox, LimiteEjecuciones limite) {
        this.sandbox = sandbox;
        this.limite = limite;
    }

    // POST y no GET: lleva código en el cuerpo y cada llamada ejecuta algo. Es una «ejecución», no un envío.
    @PostMapping("/api/ejecuciones")
    public ResponseEntity<?> ejecutar(@RequestBody Solicitud solicitud,
            @RequestAttribute("estudiante") Estudiante estudiante) {
        String codigo = solicitud.codigo();
        if (codigo == null || codigo.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "Escribe tu código antes de ejecutarlo.");
        }
        if (codigo.length() > MAX_CODIGO) {
            return error(HttpStatus.BAD_REQUEST, "El código es demasiado largo (máximo " + MAX_CODIGO + " caracteres).");
        }
        // Sin entrada se manda vacía: hay programas que no leen nada.
        String entrada = solicitud.entrada() == null ? "" : solicitud.entrada();
        if (entrada.length() > MAX_ENTRADA) {
            return error(HttpStatus.BAD_REQUEST, "La entrada es demasiado larga (máximo " + MAX_ENTRADA + " caracteres).");
        }
        String lenguaje = solicitud.lenguaje() == null ? "" : solicitud.lenguaje().trim().toLowerCase(Locale.ROOT);
        if (!LENGUAJES.contains(lenguaje)) {
            return error(HttpStatus.BAD_REQUEST, "Lenguaje no soportado. Usa python, java o javascript.");
        }

        // Después de validar: una petición inválida no llega al sandbox y por eso no gasta ejecuciones.
        long espera = limite.intentar(estudiante.getId());
        if (espera > 0) {
            return demasiadasEjecuciones(espera);
        }

        EjecucionResultado resultado;
        try {
            resultado = sandbox.ejecutar(new EjecucionSolicitud(codigo, lenguaje, entrada));
        } catch (LenguajeNoSoportadoException e) {
            // El motor activo no tiene ese lenguaje aunque user-api sí lo acepte.
            return error(HttpStatus.BAD_REQUEST, "El motor de ejecución no soporta ese lenguaje.");
        } catch (SandboxNoDisponibleException e) {
            return noDisponible();
        }

        // El motor respondió pero no pudo correr nada: fallo del servicio, no del código del estudiante.
        if (resultado.estado() == Estado.ERROR_SERVICIO) {
            return noDisponible();
        }
        // Un programa que falla, no compila o tarda demasiado es una respuesta normal (200) con su estado.
        return ResponseEntity.ok(resultado);
    }

    // 429 con Retry-After (segundos): el navegador y los clientes saben cuánto esperar sin adivinar.
    private static ResponseEntity<Detalle> demasiadasEjecuciones(long espera) {
        String tiempo = espera == 1 ? "1 segundo" : espera + " segundos";
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(espera))
                .body(new Detalle("Ejecutaste demasiadas veces seguidas. Espera " + tiempo + " e intenta de nuevo."));
    }

    private static ResponseEntity<Detalle> noDisponible() {
        return error(HttpStatus.SERVICE_UNAVAILABLE,
                "El servicio de ejecución no está disponible en este momento. Intenta de nuevo en un momento.");
    }

    private static ResponseEntity<Detalle> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(new Detalle(mensaje));
    }
}
