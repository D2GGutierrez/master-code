package com.mastercode.userapi.sandbox;

// Cuerpo de POST /execute en ai-service. La entrada va vacía si el programa no lee nada.
public record EjecucionSolicitud(String codigo, String lenguaje, String entrada) {
}
