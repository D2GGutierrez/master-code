package com.mastercode.userapi.seguridad;

import java.io.IOException;
import java.util.Locale;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mastercode.userapi.perfil.EstudianteRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Crea el perfil la primera vez que llega un estudiante con un token válido.
// Corre después de validar el Bearer: con un token rechazado nunca llega aquí.
public class AsegurarPerfilFiltro extends OncePerRequestFilter {

    private static final SimpleGrantedAuthority ESTUDIANTE = new SimpleGrantedAuthority("ROLE_ESTUDIANTE");

    private final EstudianteRepository estudiantes;

    public AsegurarPerfilFiltro(EstudianteRepository estudiantes) {
        this.estudiantes = estudiantes;
    }

    // Fuera de /api/ la petición se deniega, así que no se crea perfil.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        // Un docente también trae un token válido, pero no tiene perfil aquí (luego recibe 403).
        if (autenticacion instanceof JwtAuthenticationToken token && autenticacion.getAuthorities().contains(ESTUDIANTE)) {
            Jwt jwt = token.getToken();
            // La tabla solo admite minúsculas (CHECK de V1).
            String email = jwt.getClaimAsString("email").toLowerCase(Locale.ROOT);
            estudiantes.insertarSiNoExiste(email, jwt.getClaimAsString("institucion"));
            request.setAttribute("estudiante", estudiantes.findByEmail(email).orElseThrow());
        }
        chain.doFilter(request, response);
    }
}
