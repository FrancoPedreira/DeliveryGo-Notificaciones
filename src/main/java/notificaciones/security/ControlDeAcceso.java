package notificaciones.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// Autorización sobre el recurso: estar autenticado no alcanza. Una persona solo puede ver y modificar sus propias
// notificaciones (el id del token debe ser el destinatario); ADMIN puede todas.
// Lo usan los controllers antes de llamar al service: los services no conocen el token.
@Component
public class ControlDeAcceso {

    public void verificarAcceso(Long destinatarioId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new AccessDeniedException("Sin usuario autenticado");
        }
        boolean esAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        // JwtAuthFilter guarda como principal el id numérico que viene en el "sub" del token
        boolean esElDestinatario = destinatarioId != null && destinatarioId.equals(auth.getPrincipal());
        if (!esAdmin && !esElDestinatario) {
            // GlobalExceptionHandler la traduce a 403 FORBIDDEN
            throw new AccessDeniedException("La notificación pertenece a otro usuario");
        }
    }
}
