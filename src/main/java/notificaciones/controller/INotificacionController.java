package notificaciones.controller;

import org.springframework.http.ResponseEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import notificaciones.dto.CrearNotificacionRequestDto;
import notificaciones.dto.ErrorResponseDto;
import notificaciones.dto.NotificacionResponseDto;

// Documentación Swagger de API-01 a API-03; la implementación queda limpia en NotificacionController
@Tag(name = "Notificaciones", description = "Contrato común de notificaciones del ecosistema (CU-01, CU-04, CU-05)")
public interface INotificacionController {

    String ERROR_400 = """
            {"timestamp":"2026-10-27T19:19:11","status":400,"code":"VALIDATION_ERROR",\
            "message":"El campo canal no puede ser nulo o vacío","path":"/api/v1/notifications",\
            "correlationId":"f42e7e74-c72a-4423-b931-0a2828508aee"}""";
    String ERROR_404 = """
            {"timestamp":"2026-10-27T19:19:11","status":404,"code":"NOTIFICATION_NOT_FOUND",\
            "message":"La notificación con id 999 no existe","path":"/api/v1/notifications/999",\
            "correlationId":"a64ea3e6-5587-4a7b-bd77-005e498ae9c4"}""";

    @Operation(summary = "Crear una notificación (CU-01)", description = """
            Registra la solicitud, genera el mensaje a partir de la plantilla y los parámetros (CU-02) y lo envía \
            por el canal indicado (CU-03), todo en una sola operación.

            Responde 201 tanto si el envío salió bien (ENVIADA) como si falló (FALLIDA): un envío fallido queda \
            registrado y puede reintentarse con POST /api/v1/notifications/{id}/retry.

            Si la solicitud no pasa las validaciones (400, 422) o Usuarios no responde (503), no se registra nada.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Notificación creada, con estado ENVIADA o FALLIDA"),
            @ApiResponse(responseCode = "400", description = "Falta destinatarioId, plantillaId o canal (RN-01), "
                    + "o el JSON está mal formado", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "VALIDATION_ERROR", value = ERROR_400))),
            @ApiResponse(responseCode = "422", description = "La solicitud viola una regla de negocio", content = @Content(
                    schema = @Schema(implementation = ErrorResponseDto.class), examples = {
                            @ExampleObject(name = "CHANNEL_INACTIVE", summary = "Canal inexistente o inactivo (RN-07)",
                                    value = "{\"status\":422,\"code\":\"CHANNEL_INACTIVE\",\"message\":\"El canal SMS no existe\"}"),
                            @ExampleObject(name = "RECIPIENT_NOT_FOUND", summary = "Destinatario inexistente o inactivo (INT-01)",
                                    value = "{\"status\":422,\"code\":\"RECIPIENT_NOT_FOUND\",\"message\":\"El destinatario 999 no existe o está inactivo\"}"),
                            @ExampleObject(name = "RECIPIENT_CONTACT_MISSING", summary = "Falta el email o pushToken que pide el canal (RN-09)",
                                    value = "{\"status\":422,\"code\":\"RECIPIENT_CONTACT_MISSING\",\"message\":\"El destinatario 11 no tiene el dato de contacto que requiere el canal PUSH\"}"),
                            @ExampleObject(name = "TEMPLATE_INVALID", summary = "Plantilla inexistente, inactiva o de otro canal (RN-02)",
                                    value = "{\"status\":422,\"code\":\"TEMPLATE_INVALID\",\"message\":\"La plantilla 1 no existe, está inactiva o pertenece a otro canal\"}"),
                            @ExampleObject(name = "TEMPLATE_PARAMS_MISSING", summary = "Faltan parámetros de la plantilla (RN-08)",
                                    value = "{\"status\":422,\"code\":\"TEMPLATE_PARAMS_MISSING\",\"message\":\"Faltan los parámetros pedidoId\"}") })),
            @ApiResponse(responseCode = "503", description = "El módulo Usuarios no respondió (INT-01)", content = @Content(
                    schema = @Schema(implementation = ErrorResponseDto.class), examples = @ExampleObject(name = "USERS_SERVICE_UNAVAILABLE",
                            value = "{\"status\":503,\"code\":\"USERS_SERVICE_UNAVAILABLE\",\"message\":\"El módulo Usuarios no está disponible\"}"))) })
    ResponseEntity<NotificacionResponseDto> crearNotificacion(CrearNotificacionRequestDto notificacionDto);

    @Operation(summary = "Consultar el estado de una notificación (CU-04)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actual de la notificación"),
            @ApiResponse(responseCode = "400", description = "El id no es numérico", content = @Content(
                    schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "No existe una notificación con ese id", content = @Content(
                    schema = @Schema(implementation = ErrorResponseDto.class), examples = @ExampleObject(
                            name = "NOTIFICATION_NOT_FOUND", value = ERROR_404))) })
    ResponseEntity<NotificacionResponseDto> consultarEstado(
            @Parameter(description = "Id de la notificación", example = "1") Long id);

    @Operation(summary = "Reintentar una notificación fallida (CU-05)", description = """
            Solo se puede reintentar una notificación en estado FALLIDA. Cada reintento registra un nuevo intento; \
            al llegar al máximo (3 intentos) la notificación queda FALLIDA_DEFINITIVA y ya no se puede reintentar.

            Responde 200 con el estado resultante: ENVIADA, FALLIDA o FALLIDA_DEFINITIVA.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reintento procesado; el estado indica el resultado"),
            @ApiResponse(responseCode = "404", description = "No existe una notificación con ese id", content = @Content(
                    schema = @Schema(implementation = ErrorResponseDto.class), examples = @ExampleObject(
                            name = "NOTIFICATION_NOT_FOUND", value = ERROR_404))),
            @ApiResponse(responseCode = "409", description = "La notificación no se puede reintentar en su estado actual",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), examples = {
                            @ExampleObject(name = "NOTIFICATION_NOT_RETRYABLE", summary = "No está FALLIDA (RN-05)",
                                    value = "{\"status\":409,\"code\":\"NOTIFICATION_NOT_RETRYABLE\",\"message\":\"La notificación 1 está en estado ENVIADA y solo puede reintentarse si está FALLIDA\"}"),
                            @ExampleObject(name = "MAX_RETRIES_EXCEEDED", summary = "Ya alcanzó el máximo de intentos (RN-04)",
                                    value = "{\"status\":409,\"code\":\"MAX_RETRIES_EXCEEDED\",\"message\":\"La notificación 1 alcanzó el máximo de intentos y ya no puede reintentarse\"}") })) })
    ResponseEntity<NotificacionResponseDto> reintentarNotificacion(
            @Parameter(description = "Id de la notificación", example = "1") Long id);
}
