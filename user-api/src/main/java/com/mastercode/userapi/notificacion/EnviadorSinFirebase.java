package com.mastercode.userapi.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// El enviador de hoy: todavía no hay Firebase (H5), así que no envía nada. Responde false para que el aviso no se dé por
// entregado ni se marque el día. No escribe el token en el registro: es un dato del teléfono.
@Component
public class EnviadorSinFirebase implements EnviadorDeAvisos {

    private static final Logger registro = LoggerFactory.getLogger(EnviadorSinFirebase.class);

    @Override
    public boolean enviar(String token, String titulo, String mensaje) {
        registro.debug("Aviso sin enviar (todavía no hay Firebase): {}", titulo);
        return false;
    }
}
