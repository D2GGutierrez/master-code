package com.mastercode.userapi.admin;

// admin-api responde 404 al pedir un problema: no existe, no está aprobado o fue descartado.
// A propósito no distingue los casos: para el estudiante es lo mismo (HU31-T2).
public class ProblemaNoVisibleException extends RuntimeException {
}
