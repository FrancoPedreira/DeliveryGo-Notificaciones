package notificaciones.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// Solo para tests: genera tokens como los emitiría Usuarios y hace que el MockMvc de todos los tests
// envíe por defecto un token ADMIN (un test puede pisarlo mandando su propio header Authorization)
@Component
public class TokenDePrueba implements MockMvcBuilderCustomizer {
    private final String secret;

    public TokenDePrueba(@Value("${notificaciones.jwt.secret}") String secret) {
        this.secret = secret;
    }

    public String crear(Long usuarioId, String rol) {
        return crear(secret, usuarioId, rol, new Date(System.currentTimeMillis() + 3_600_000));
    }

    public static String crear(String secret, Long usuarioId, String rol, Date vencimiento) {
        return Jwts.builder().subject(usuarioId.toString()).claim("role", rol).expiration(vencimiento)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
    }

    @Override
    public void customize(ConfigurableMockMvcBuilder<?> builder) {
        builder.defaultRequest(get("/").header("Authorization", "Bearer " + crear(1L, "ADMIN")));
    }
}
