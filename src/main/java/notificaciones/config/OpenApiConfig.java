package notificaciones.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

// Swagger UI en /swagger-ui.html y el contrato OpenAPI en /v3/api-docs
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificacionesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DeliveryGo - Módulo Notificaciones")
                        .version("1.0")
                        .description("""
                                Contrato común para que cualquier módulo del ecosistema (Usuarios, Pedidos, Pagos, \
                                Logística) solicite una notificación indicando destinatario, plantilla, canal y \
                                parámetros, sin lógica particular por tipo de evento.

                                **Datos de prueba cargados al iniciar**

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

                                Todos los errores devuelven el mismo formato (ErrorResponseDto). Si se envía el header \
                                `X-Correlation-Id`, se devuelve el mismo valor en la respuesta de error."""));
    }
}
