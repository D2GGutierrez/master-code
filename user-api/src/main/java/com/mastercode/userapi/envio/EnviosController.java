package com.mastercode.userapi.envio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mastercode.userapi.Detalle;
import com.mastercode.userapi.admin.AdminCliente;
import com.mastercode.userapi.admin.AdminNoDisponibleException;
import com.mastercode.userapi.admin.ProblemaCalificableDto;
import com.mastercode.userapi.admin.ProblemaNoVisibleException;
import com.mastercode.userapi.inscripcion.InscripcionRepository;
import com.mastercode.userapi.perfil.Estudiante;
import com.mastercode.userapi.sandbox.EjecucionResultado;
import com.mastercode.userapi.sandbox.EjecucionSolicitud;
import com.mastercode.userapi.sandbox.Estado;
import com.mastercode.userapi.sandbox.LenguajeNoSoportadoException;
import com.mastercode.userapi.sandbox.SandboxCliente;
import com.mastercode.userapi.sandbox.SandboxNoDisponibleException;

// HU36 y HU38: el envío oficial. Ejecuta el código del estudiante contra TODOS los casos del problema (ocultos
// incluidos), lo califica, guarda el intento en app_envio y devuelve el detalle. A diferencia de «Ejecutar» (HU06),
// este sí cuenta como intento. Al estudiante le llega entrada y salida solo de los casos públicos; de uno oculto,
// únicamente si pasó o falló (HU39).
@RestController
public class EnviosController {

    private static final String ESCRITURA_CODIGO = "escritura_codigo";
    // Mismo tope que «Ejecutar»: no se mandan al sandbox cuerpos enormes.
    private static final int MAX_CODIGO = 50_000;
    // Tope de la salida y del error que se devuelven por caso: un programa que imprime sin parar no hincha la respuesta.
    private static final int MAX_TEXTO = 2_000;

    record Solicitud(String codigo) {
    }

    // Un caso oculto lleva solo publico y paso: los demás campos van vacíos y NON_NULL los quita del JSON.
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CasoCalificado(boolean publico, boolean paso, String entrada, String salidaEsperada, String salidaObtenida,
            String error, Estado estado) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record EnvioCalificado(long id, int numeroIntento, String resultado, BigDecimal puntaje, int casosSuperados,
            int casosTotal, String aviso, List<CasoCalificado> casos) {
    }

    private final AdminCliente admin;
    private final InscripcionRepository inscripciones;
    private final SandboxCliente sandbox;
    private final EnvioRepository envios;

    public EnviosController(AdminCliente admin, InscripcionRepository inscripciones, SandboxCliente sandbox,
            EnvioRepository envios) {
        this.admin = admin;
        this.inscripciones = inscripciones;
        this.sandbox = sandbox;
        this.envios = envios;
    }

    // POST: cada llamada ejecuta código y deja un intento guardado. El estudiante sale del token, nunca del cuerpo.
    @PostMapping("/api/problemas/{problemaId}/envios")
    public ResponseEntity<?> enviar(@PathVariable long problemaId, @RequestBody Solicitud solicitud,
            @AuthenticationPrincipal Jwt jwt, @RequestAttribute("estudiante") Estudiante estudiante) {
        String codigo = solicitud.codigo();
        if (codigo == null || codigo.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "Escribe tu código antes de enviarlo.");
        }
        if (codigo.length() > MAX_CODIGO) {
            return error(HttpStatus.BAD_REQUEST, "El código es demasiado largo (máximo " + MAX_CODIGO + " caracteres).");
        }

        ProblemaCalificableDto problema;
        try {
            // admin-api decide qué problemas son visibles (D5): uno sin aprobar o descartado da 404 aquí también.
            problema = admin.problemaParaCalificar(problemaId);
        } catch (ProblemaNoVisibleException e) {
            return noEncontrado();
        } catch (AdminNoDisponibleException e) {
            return error(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo cargar el problema en este momento. Intenta de nuevo.");
        }
        // Mismo 404 que un problema inexistente: no se revela que existe un problema de un curso ajeno.
        if (!inscripciones.estaInscrito(estudiante.getId(), problema.cursoId())) {
            return noEncontrado();
        }
        if (!ESCRITURA_CODIGO.equals(problema.tipoEjercicio())) {
            return error(HttpStatus.BAD_REQUEST, "Este ejercicio no se resuelve escribiendo código.");
        }
        List<ProblemaCalificableDto.Caso> casos = problema.casosPrueba() == null ? List.of() : problema.casosPrueba();
        if (casos.isEmpty()) {
            return error(HttpStatus.CONFLICT, "Este problema todavía no tiene casos de prueba.");
        }
        // El lenguaje es el del problema, no uno que elija el estudiante: así el código se evalúa como lo pensó el docente.
        String lenguaje = problema.lenguaje() == null ? "" : problema.lenguaje().trim().toLowerCase(Locale.ROOT);

        List<CasoCalificado> evaluados = new ArrayList<>();
        List<EjecucionResultado> resultados = new ArrayList<>();
        int superados = 0;
        for (ProblemaCalificableDto.Caso caso : casos) {
            EjecucionResultado resultado;
            try {
                resultado = sandbox.ejecutar(new EjecucionSolicitud(codigo, lenguaje,
                        caso.entrada() == null ? "" : caso.entrada()));
            } catch (LenguajeNoSoportadoException e) {
                return error(HttpStatus.BAD_REQUEST, "El motor de ejecución no soporta el lenguaje de este problema.");
            } catch (SandboxNoDisponibleException e) {
                return noDisponible();
            }
            // Falló el motor, no el código del estudiante: no se guarda nada, el intento no se le cuenta.
            if (resultado.estado() == Estado.ERROR_SERVICIO) {
                return noDisponible();
            }
            boolean paso = resultado.estado() == Estado.OK && Calificacion.coincide(caso.salidaEsperada(), resultado.salida());
            if (paso) {
                superados++;
            }
            resultados.add(resultado);
            evaluados.add(caso.esPublico() ? casoPublico(caso, resultado, paso) : new CasoCalificado(false, paso, null,
                    null, null, null, null));
        }

        int total = casos.size();
        String resultado = Calificacion.resultado(superados, total);
        BigDecimal puntaje = Calificacion.puntaje(superados, total);
        // El claim cliente lo pone admin-api al iniciar sesión (user-web o mobile); el estudiante no lo escribe.
        String plataforma = "mobile".equals(jwt.getClaimAsString("cliente")) ? "movil" : "web";
        EnvioRepository.Fila fila = envios.registrar(estudiante.getId(), problemaId, plataforma, codigo, resultado, puntaje);

        return ResponseEntity.status(HttpStatus.CREATED).body(new EnvioCalificado(fila.id(), fila.numeroIntento(),
                resultado, puntaje, superados, total, aviso(resultados), evaluados));
    }

    // Lo que ve el estudiante de un caso público: su entrada, lo esperado y lo que imprimió su programa.
    private static CasoCalificado casoPublico(ProblemaCalificableDto.Caso caso, EjecucionResultado resultado,
            boolean paso) {
        String error = resultado.error() == null || resultado.error().isBlank() ? null : recortar(resultado.error());
        return new CasoCalificado(true, paso, caso.entrada(), caso.salidaEsperada(), recortar(resultado.salida()),
                error, resultado.estado());
    }

    // Un mensaje general para distinguir un error del código del estudiante de un caso que simplemente no coincide.
    // Solo un error de compilación muestra el texto del motor: no depende de la entrada, así que no puede revelar un
    // caso oculto. Con errores de ejecución o tiempo el mensaje es fijo (esos textos podrían imprimir la entrada).
    private static String aviso(List<EjecucionResultado> resultados) {
        for (EjecucionResultado resultado : resultados) {
            if (resultado.estado() == Estado.ERROR_COMPILACION) {
                String detalle = resultado.error() == null || resultado.error().isBlank() ? "" : " " + recortar(resultado.error());
                return "Tu código no compiló." + detalle;
            }
        }
        for (EjecucionResultado resultado : resultados) {
            if (resultado.estado() == Estado.TIEMPO_AGOTADO) {
                return "Tu programa tardó demasiado en uno o más casos y se detuvo.";
            }
        }
        for (EjecucionResultado resultado : resultados) {
            if (resultado.estado() == Estado.ERROR_EJECUCION) {
                return "Tu programa falló al ejecutarse en uno o más casos.";
            }
        }
        return null;
    }

    private static String recortar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= MAX_TEXTO ? texto : texto.substring(0, MAX_TEXTO) + "…";
    }

    private static ResponseEntity<Detalle> noEncontrado() {
        return error(HttpStatus.NOT_FOUND, "Problema no encontrado.");
    }

    private static ResponseEntity<Detalle> noDisponible() {
        return error(HttpStatus.SERVICE_UNAVAILABLE,
                "El servicio de ejecución no está disponible en este momento. Tu envío no se contó: intenta de nuevo en un momento.");
    }

    private static ResponseEntity<Detalle> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(new Detalle(mensaje));
    }
}
