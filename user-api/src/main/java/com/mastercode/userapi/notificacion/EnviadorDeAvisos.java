package com.mastercode.userapi.notificacion;

// HU52-T2: quien entrega un aviso a un teléfono. Hoy solo existe EnviadorSinFirebase; el envío real (Firebase Cloud
// Messaging, H5) será otra implementación que lo reemplace sin tocar la decisión de a quién avisar.
public interface EnviadorDeAvisos {

    // true si el aviso salió hacia el teléfono; false si no se pudo (o no hay cómo enviarlo todavía). No lanza: un token
    // que falla no debe impedir el aviso a los demás.
    boolean enviar(String token, String titulo, String mensaje);
}
