package notificaciones.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

// Swagger UI en /swagger-ui.html y el contrato OpenAPI en /v3/api-docs
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificacionesOpenAPI() {
        return new OpenAPI()
                // Botón "Authorize" de Swagger UI: permite pegar el JWT y lo envía en todas las llamadas
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .info(new Info()
                        .title("DeliveryGo - Módulo Notificaciones")
                        .version("1.0")
                        .description("""
                                Contrato común para que cualquier módulo del ecosistema (Usuarios, Pedidos, Pagos, \
                                Logística) solicite una notificación indicando destinatario, plantilla, canal (opcional) y \
                                parámetros, sin lógica particular por tipo de evento.

                                Además del contrato común expone el historial por usuario, la administración de \
                                plantillas y canales, la consulta de intentos y la bandeja interna (marcar como leída).

                                **Datos de prueba cargados al iniciar** (las plantillas se pueden listar con \
                                GET /api/v1/templates)

                                | plantillaId | Nombre | Canal | Parámetros que requiere |
                                |---|---|---|---|
                                | 1 | PEDIDO_CONFIRMADO_EMAIL | EMAIL | nombre, pedidoId |
                                | 2 | PAGO_APROBADO_PUSH | PUSH | monto |
                                | 3 | ENVIO_EN_CAMINO_IN_APP | IN_APP | codigo |
                                | 4 | USUARIO_BIENVENIDA_EMAIL | EMAIL | nombre |
                                | 5 | RECUPERACION_PASSWORD_EMAIL | EMAIL | nombre, token |

                                | destinatarioId | Usuario simulado |
                                |---|---|
                                | 10 | Activo, con email y pushToken |
                                | 11 | Activo, con email y sin pushToken |
                                | 12 | Inactivo |

                                El envío por EMAIL y PUSH es simulado y falla aproximadamente 3 de cada 10 veces, \
                                para poder probar el reintento. IN_APP siempre se entrega.

                                **INT-01 — Usuarios:** el destinatario se valida con un cliente simulado (modo \
                                predeterminado, usuarios 10, 11 y 12) o con el módulo Usuarios real \
                                (`notificaciones.usuarios.modo=http`), que se consulta con GET /api/v1/users/{id} sin \
                                credencial y propagando el X-Correlation-Id. Convierte estado == ACTIVO en el \
                                indicador activo; Usuarios no publica pushToken. `GET /status` informa el modo activo.

                                Todos los errores devuelven el mismo formato (ErrorResponseDto). Todas las respuestas \
                                devuelven el header `X-Correlation-Id`: el que envió el llamador o uno generado.

                                **Seguridad:** las APIs requieren `Authorization: Bearer <JWT>` (botón \
                                Authorize), salvo `GET /api/v1/templates` y `POST /api/v1/notifications`, que otros \
                                módulos llaman sin credencial por acuerdo del ecosistema. El token lo emite Usuarios; \
                                este módulo solo lo valida: `sub` es el id numérico del usuario y `role` es CLIENTE, \
                                COMERCIO, REPARTIDOR o ADMIN. Crear o modificar plantillas y habilitar o deshabilitar \
                                canales requiere ADMIN. Cada usuario solo ve y modifica sus propias notificaciones \
                                (historial, estado, intentos, reintento y lectura); ADMIN puede ver todas."""));
    }

    // Todas las APIs pueden responder 401 y 403: se documentan acá una sola vez en lugar de repetirlo en cada una
    @Bean
    public OpenApiCustomizer respuestasDeSeguridad() {
        // /status es público: no lleva 401 ni 403
        return openApi -> openApi.getPaths().forEach((ruta, path) -> {
            if ("/status".equals(ruta)) {
                return;
            }
            path.readOperations().forEach(operacion -> {
                operacion.getResponses().addApiResponse("401",
                        new ApiResponse().description("Falta el token o es inválido (UNAUTHORIZED)"));
                operacion.getResponses().addApiResponse("403", new ApiResponse().description(
                        "El rol del token no puede hacer esta operación o la notificación es de otro usuario (FORBIDDEN)"));
            });
        });
    }
}
