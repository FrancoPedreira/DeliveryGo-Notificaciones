package notificaciones.security;

import java.io.IOException;
import java.util.List;
import java.util.Set;

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

// Lee el header "Authorization: Bearer <token>" y valida el JWT emitido por Usuarios.
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final String PREFIJO = "Bearer ";
    // SISTEMA: token de servicio que usa Usuarios cuando no hay un usuario logueado (bienvenida y recuperación)
    private static final Set<String> ROLES_VALIDOS = Set.of("CLIENTE", "COMERCIO", "REPARTIDOR", "ADMIN", "SISTEMA");

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

                // En el contrato de Usuarios, "sub" contiene el id numérico del usuario.
                Object subject = claims.get(Claims.SUBJECT);
                if (subject == null) {
                    throw new IllegalArgumentException("El token no contiene el id del usuario");
                }
                Long usuarioId = Long.valueOf(subject.toString());

                // El token trae el rol sin ROLE_; Spring Security necesita ese prefijo internamente.
                String rol = claims.get("role", String.class);
                if (rol == null || !ROLES_VALIDOS.contains(rol)) {
                    throw new IllegalArgumentException("El token contiene un rol inválido");
                }

                String autoridad = "ROLE_" + rol;
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        usuarioId, token, List.of(new SimpleGrantedAuthority(autoridad))));
            } catch (JwtException | IllegalArgumentException e) {
                // Firma inválida, token vencido, id no numérico o rol inválido: queda sin autenticar.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
