package notificaciones.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import notificaciones.dto.CanalDto;
import notificaciones.dto.CanalEstadoRequestDto;
import notificaciones.dto.ErrorResponseDto;

// Documentación Swagger de API-09 y API-10; la implementación queda limpia en CanalController
@Tag(name = "Canales", description = "Canales de envío EMAIL, PUSH e IN_APP (CU-08)")
public interface ICanalController {

    @Operation(summary = "Listar los canales (CU-08)", description = "Los canales disponibles para el campo canal de POST /api/v1/notifications y si están habilitados.")
    @ApiResponse(responseCode = "200", description = "EMAIL, PUSH e IN_APP con su estado")
    ResponseEntity<List<CanalDto>> listarCanales();

    @Operation(summary = "Habilitar o deshabilitar un canal (CU-08)", description = """
            Con el canal deshabilitado no se aceptan solicitudes nuevas por ese canal (CHANNEL_INACTIVE). Las \
            notificaciones ya registradas no cambian, pero sus reintentos fallan mientras el canal siga inactivo. \
            Es idempotente.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Canal con su estado actual"),
            @ApiResponse(responseCode = "400", description = "Falta el campo activo", content = @Content(
                    schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "El canal no existe", content = @Content(
                    schema = @Schema(implementation = ErrorResponseDto.class), examples = @ExampleObject(
                            name = "CHANNEL_NOT_FOUND", value = "{\"status\":404,\"code\":\"CHANNEL_NOT_FOUND\",\"message\":\"El canal SMS no existe\"}"))) })
    ResponseEntity<CanalDto> cambiarEstadoCanal(
            @Parameter(description = "Tipo de canal", example = "PUSH", schema = @Schema(allowableValues = { "EMAIL",
                    "PUSH", "IN_APP" })) String tipo,
            CanalEstadoRequestDto estadoDto);
}
