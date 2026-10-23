package com.mastercode.userapi.seguridad;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.mastercode.userapi.perfil.EstudianteRepository;

// user-api solo verifica los JWT que firma admin-api (ADR-4): no tiene login ni la clave privada.
@Configuration
public class SeguridadConfig {

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, EstudianteRepository estudiantes,
            @Value("${internal.api-key}") String claveInterna) throws Exception {
        return http
                // Sin cookies ni sesión: cada petición trae su Bearer, así que CSRF no aplica.
                .csrf(csrf -> csrf.disable())
                // Dentro de la cadena: así el preflight OPTIONS del navegador se responde antes de pedir el JWT.
                .cors(Customizer.withDefaults())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers("/health").permitAll()
                        // Sin JWT: la protege ClaveInternaFiltro con la clave interna (D3).
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers("/api/**").hasRole("ESTUDIANTE")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(servidor -> servidor.jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDeRol())))
                // Se crean aquí y no como @Component para que Spring Boot no los registre dos veces.
                .addFilterBefore(new ClaveInternaFiltro(claveInterna), BearerTokenAuthenticationFilter.class)
                .addFilterAfter(new AsegurarPerfilFiltro(estudiantes), BearerTokenAuthenticationFilter.class)
                .build();
    }

    // Orígenes de user-web por variable de entorno (ADR-7 / D4), igual que en admin-api.
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${cors.allowed-origins}") List<String> origenes) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origenes);
        cors.setAllowedMethods(List.of(
                HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(), HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name()));
        cors.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        // Sin esto el navegador oculta Retry-After del 429 de «Ejecutar» (HU06-T4) a user-web.
        cors.setExposedHeaders(List.of(HttpHeaders.RETRY_AFTER));
        // El token va en Authorization, no en cookies.
        cors.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", cors);
        return fuente;
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${jwt.jwks-uri}") String jwksUri, @Value("${jwt.issuer}") String issuer) {
        // Timeouts cortos: si admin-api cae, la petición falla rápido en vez de quedarse colgada.
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        fabrica.setReadTimeout(Duration.ofSeconds(3));

        // El JWKS se descarga en la primera validación, no al arrancar: user-api puede arrancar antes que admin-api.
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .restOperations(new RestTemplate(fabrica))
                .build();

        Predicate<String> conTexto = valor -> valor != null && !valor.isBlank();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                // 5 s y no los 60 s por defecto: el access dura solo 15 min.
                new JwtTimestampValidator(Duration.ofSeconds(5)),
                new JwtIssuerValidator(issuer),
                // simplejwt firma el refresh con la misma clave e iss: sin esto, un refresh abriría la API.
                new JwtClaimValidator<String>("token_type", "access"::equals),
                // Sin email o institucion no se puede crear el perfil.
                new JwtClaimValidator<String>("email", conTexto),
                new JwtClaimValidator<String>("institucion", conTexto)));
        return decoder;
    }

    // rol "estudiante" -> ROLE_ESTUDIANTE, que es lo que pide hasRole("ESTUDIANTE").
    private static JwtAuthenticationConverter conversorDeRol() {
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(jwt -> {
            String rol = jwt.getClaimAsString("rol");
            return rol == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase(Locale.ROOT)));
        });
        return conversor;
    }
}
