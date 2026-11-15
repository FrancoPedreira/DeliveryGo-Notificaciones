package notificaciones.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import notificaciones.config.CorrelationIdFilter;
import notificaciones.dto.UsuarioContactoResponseDto;
import notificaciones.exception.ServicioNoDisponibleException;

// INT-01 real: llama a GET {base-url}/api/v1/users/{id}/contact del módulo Usuarios. Se activa con
// notificaciones.usuarios.modo=http (por defecto se usa UsuariosClientSimulado).
// 200 -> contacto, 404 -> vacío; timeout, error de conexión, 5xx o cualquier otra respuesta -> 503
@Component
@ConditionalOnProperty(name = "notificaciones.usuarios.modo", havingValue = "http")
public class UsuariosClientHttp implements UsuariosClient {
    private static final Logger log = LoggerFactory.getLogger(UsuariosClientHttp.class);
    private static final String CODIGO_NO_DISPONIBLE = "USERS_SERVICE_UNAVAILABLE";
    private static final String MENSAJE_NO_DISPONIBLE = "El módulo Usuarios no está disponible";

    private final RestClient restClient;

    @Autowired
    public UsuariosClientHttp(@Value("${notificaciones.usuarios.base-url}") String baseUrl,
            @Value("${notificaciones.usuarios.timeout-ms:2000}") long timeoutMs) {
        this(construirRestClient(RestClient.builder(), baseUrl, timeoutMs));
    }

    // Permite inyectar un RestClient ya armado (los tests lo enlazan a un MockRestServiceServer)
    UsuariosClientHttp(RestClient restClient) {
        this.restClient = restClient;
    }

    // Mismo timeout para conectar y para esperar la respuesta
    private static RestClient construirRestClient(RestClient.Builder builder, String baseUrl, long timeoutMs) {
        Duration timeout = Duration.ofMillis(timeoutMs);
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(timeout).build());
        requestFactory.setReadTimeout(timeout);
        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public Optional<UsuarioContactoResponseDto> obtenerContacto(Long usuarioId) {
        try {
            return restClient.get()
                    .uri("/api/v1/users/{id}/contact", usuarioId)
                    .headers(headers -> {
                        // Propaga el id de la solicitud entrante (lo deja CorrelationIdFilter en el MDC)
                        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
                        if (correlationId != null) {
                            headers.set(CorrelationIdFilter.HEADER, correlationId);
                        }
                        // Reenvía el JWT de la solicitud entrante (lo deja JwtAuthFilter como credencial)
                        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
                        if (autenticacion != null && autenticacion.getCredentials() instanceof String token) {
                            headers.setBearerAuth(token);
                        }
                    })
                    .exchange((request, response) -> {
                        HttpStatus status = HttpStatus.resolve(response.getStatusCode().value());
                        if (status == HttpStatus.OK) {
                            return Optional.ofNullable(response.bodyTo(UsuarioContactoResponseDto.class));
                        }
                        if (status == HttpStatus.NOT_FOUND) {
                            return Optional.<UsuarioContactoResponseDto>empty();
                        }
                        log.warn("Usuarios respondió {} al consultar el contacto de {}",
                                response.getStatusCode(), usuarioId);
                        throw new ServicioNoDisponibleException(CODIGO_NO_DISPONIBLE, MENSAJE_NO_DISPONIBLE);
                    });
        } catch (RestClientException e) {
            // Timeout, conexión rechazada o cuerpo ilegible
            log.warn("No se pudo consultar a Usuarios el contacto de {}: {}", usuarioId, e.getMessage());
            throw new ServicioNoDisponibleException(CODIGO_NO_DISPONIBLE, MENSAJE_NO_DISPONIBLE);
        }
    }
}
