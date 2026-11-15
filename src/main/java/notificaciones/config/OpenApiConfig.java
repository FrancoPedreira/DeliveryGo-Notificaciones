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

                                | plantillaId | Canal | Parámetros que requiere |
                                |---|---|---|
                                | 1 | EMAIL | nombre, pedidoId |
                                | 2 | PUSH | monto |
                                | 3 | IN_APP | codigo |

                                | destinatarioId | Usuario simulado |
                                |---|---|
                                | 10 | Activo, con email y pushToken |
                                | 11 | Activo, con email y sin pushToken |
                                | 12 | Inactivo |

                                El envío por EMAIL y PUSH es simulado y falla aproximadamente 3 de cada 10 veces, \
                                para poder probar el reintento. IN_APP siempre se entrega.

                                La validación del destinatario contra Usuarios (INT-01) es momentánea: se usa un \
                                cliente simulado hasta que el equipo de Usuarios publique su API.

                                Todos los errores devuelven el mismo formato (ErrorResponseDto). Todas las respuestas \
                                devuelven el header `X-Correlation-Id`: el que envió el llamador o uno generado.

                                **Seguridad:** todas las APIs requieren `Authorization: Bearer <JWT>` (botón \
                                Authorize). El token lo emite el módulo Usuarios; este módulo solo lo valida. \
                                Crear o modificar plantillas y habilitar o deshabilitar canales requiere el rol ADMIN."""));
    }

    // Todas las APIs pueden responder 401 y 403: se documentan acá una sola vez en lugar de repetirlo en cada una
    @Bean
    public OpenApiCustomizer respuestasDeSeguridad() {
        return openApi -> openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operacion -> {
            operacion.getResponses().addApiResponse("401",
                    new ApiResponse().description("Falta el token o es inválido (UNAUTHORIZED)"));
            operacion.getResponses().addApiResponse("403",
                    new ApiResponse().description("El rol del token no puede hacer esta operación (FORBIDDEN)"));
        }));
    }
}
