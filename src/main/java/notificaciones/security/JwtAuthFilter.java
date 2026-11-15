package notificaciones.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Lee el header "Authorization: Bearer <token>". Si el token es válido deja al usuario autenticado, con su rol,
// en el contexto de Spring Security. Si falta o es inválido no hace nada: SecurityConfig responde 401
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIJO)) {
            String token = header.substring(PREFIJO.length());
            try {
                Claims claims = jwtService.validar(token);
                // Spring Security espera los roles con el prefijo ROLE_ (hasRole("ADMIN") busca ROLE_ADMIN)
                String rol = "ROLE_" + claims.get("role", String.class);
                // El token queda como credencial para poder reenviarlo al llamar a Usuarios (INT-01)
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), token, List.of(new SimpleGrantedAuthority(rol))));
            } catch (JwtException | IllegalArgumentException e) {
                // Firma inválida, token vencido o mal formado: la solicitud sigue sin autenticar
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
