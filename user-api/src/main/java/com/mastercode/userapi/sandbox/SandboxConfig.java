package com.mastercode.userapi.sandbox;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class SandboxConfig {

    @Bean
    SandboxCliente sandboxCliente(
            @Value("${sandbox.url}") String url,
            @Value("${sandbox.timeout-segundos}") int timeoutSegundos) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        // Sin tiempo máximo, un ai-service colgado dejaría colgada también la petición del estudiante.
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        return new SandboxCliente(RestClient.builder().baseUrl(url).requestFactory(fabrica).build());
    }
}
