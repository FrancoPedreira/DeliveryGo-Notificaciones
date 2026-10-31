package notificaciones.controller;

import org.springframework.http.ResponseEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import notificaciones.dto.ErrorResponseDto;
import notificaciones.dto.NotificacionResponseDto;
import notificaciones.dto.PaginaResponseDto;

// Documentación Swagger de API-04; la implementación queda limpia en HistorialController
@Tag(name = "Historial", description = "Historial de notificaciones de un usuario (CU-06)")
public interface IHistorialController {

    @Operation(summary = "Consultar el historial de notificaciones de un usuario (CU-06)", description = """
            Devuelve las notificaciones del destinatario, las más recientes primero, paginadas (page, size, sort). \
            Cada elemento incluye el mensaje generado. Se puede filtrar por estado, canal y, para la bandeja \
            interna, por leídas o no leídas.

            Un usuario sin notificaciones devuelve una página vacía. No consulta al módulo Usuarios.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página del historial, posiblemente vacía"),
            @ApiResponse(responseCode = "400", description = "Filtro, paginación u orden inválidos, o el id no es numérico",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))) })
    ResponseEntity<PaginaResponseDto<NotificacionResponseDto>> consultarHistorial(
            @Parameter(description = "Id del usuario destinatario", example = "10") Long id,
            @Parameter(description = "Filtra por estado", example = "ENVIADA", schema = @Schema(allowableValues = {
                    "PENDIENTE", "ENVIADA", "FALLIDA", "FALLIDA_DEFINITIVA" })) String estado,
            @Parameter(description = "Filtra por canal", example = "IN_APP", schema = @Schema(allowableValues = { "EMAIL",
                    "PUSH", "IN_APP" })) String canal,
            @Parameter(description = "true: solo leídas; false: solo no leídas (CU-10)") Boolean leida,
            @Parameter(description = "Número de página, desde 0") int page,
            @Parameter(description = "Elementos por página, de 1 a 100") int size,
            @Parameter(description = "campo,asc|desc. Campos: id, fechaCreacion, fechaUltimoIntento, estado, cantidadIntentos",
                    example = "fechaCreacion,desc") String sort);
}
