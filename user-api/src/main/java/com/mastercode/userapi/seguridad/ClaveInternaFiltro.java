package com.mastercode.userapi.seguridad;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// D3: las rutas /internal/ las usa admin-api, no una persona. En vez de JWT llevan la cabecera
// X-Internal-Key con una clave que solo conocen admin-api y user-api.
public class ClaveInternaFiltro extends OncePerRequestFilter {

    private final byte[] clave;

    public ClaveInternaFiltro(String clave) {
        this.clave = clave == null ? new byte[0] : clave.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String enviada = request.getHeader("X-Internal-Key");
        // Sin clave configurada se niega todo: es mejor que dejar la API interna abierta por olvido.
        // MessageDigest.isEqual no se corta en el primer carácter distinto, así el tiempo de
        // respuesta no revela cuántos caracteres coinciden.
        boolean valida = clave.length > 0 && enviada != null
                && MessageDigest.isEqual(clave, enviada.getBytes(StandardCharsets.UTF_8));
        if (!valida) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}
