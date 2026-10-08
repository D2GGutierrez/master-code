package com.mastercode.userapi.seguridad;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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

import com.mastercode.userapi.perfil.EstudianteRepository;

// user-api solo verifica los JWT que firma admin-api (ADR-4): no tiene login ni la clave privada.
@Configuration
public class SeguridadConfig {

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, EstudianteRepository estudiantes) throws Exception {
        return http
                // Sin cookies ni sesión: cada petición trae su Bearer, así que CSRF no aplica.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/api/**").hasRole("ESTUDIANTE")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(servidor -> servidor.jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDeRol())))
                // Se crea aquí y no como @Component para que Spring Boot no lo registre dos veces.
                .addFilterAfter(new AsegurarPerfilFiltro(estudiantes), BearerTokenAuthenticationFilter.class)
                .build();
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
