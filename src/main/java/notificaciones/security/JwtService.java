package notificaciones.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// Notificaciones NO emite tokens: los emite el módulo Usuarios en el login. Acá solo se validan,
// usando la misma clave secreta con la que Usuarios los firma (HS256)
@Service
public class JwtService {
    private final SecretKey clave;

    public JwtService(@Value("${notificaciones.jwt.secret}") String secret) {
        this.clave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Verifica la firma y el vencimiento (exp). Si el token es inválido o está vencido lanza JwtException.
    // Devuelve los claims: "sub" es el usuario y "role" su rol (por ejemplo ADMIN)
    public Claims validar(String token) {
        return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
    }
}
