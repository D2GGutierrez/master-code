package com.mastercode.userapi.admin;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AdminConfig {

    @Bean
    AdminCliente adminCliente(
            @Value("${admin.url}") String url,
            @Value("${internal.api-key}") String claveInterna) {
        // Tiempos cortos: si admin-api cae, la petición del estudiante falla rápido y no se queda colgada.
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        return new AdminCliente(RestClient.builder()
                .baseUrl(url)
                .requestFactory(fabrica)
                .defaultHeader("X-Internal-Key", claveInterna)
                .build());
    }
}
