package com.mastercode.userapi.ejercicio;

// El resultado de corregir una respuesta. respuestaCorrecta solo se llena cuando el estudiante falló: es lo que HU48-T2
// le muestra después de responder; si acertó es null.
public record Veredicto(boolean correcta, String respuestaCorrecta) {
}
