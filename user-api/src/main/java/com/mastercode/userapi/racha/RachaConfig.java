package com.mastercode.userapi.racha;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// El reloj de la racha: un bean para que las pruebas puedan poner la hora que quieran.
@Configuration
public class RachaConfig {

    @Bean
    Clock relojDeRacha() {
        return Clock.systemUTC();
    }
}
