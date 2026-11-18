package com.mastercode.userapi.partida;

// Cómo se llama un compañero en la app: la parte del correo antes de la @. El correo completo no sale de user-api hacia
// otros estudiantes.
final class Alias {

    private Alias() {
    }

    static String de(String email) {
        if (email == null) {
            return "";
        }
        int arroba = email.indexOf('@');
        return arroba > 0 ? email.substring(0, arroba) : email;
    }
}
