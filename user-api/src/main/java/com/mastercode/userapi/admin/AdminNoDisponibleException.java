package com.mastercode.userapi.admin;

// admin-api no respondió, falló (5xx), rechazó la clave interna o devolvió algo ilegible.
public class AdminNoDisponibleException extends RuntimeException {

    public AdminNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
