package com.mastercode.userapi;

// Cuerpo de las respuestas de error de la API: {"detail": "..."}, igual que admin-api (DRF).
public record Detalle(String detail) {
}
