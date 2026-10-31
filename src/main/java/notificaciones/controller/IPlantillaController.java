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
import notificaciones.dto.ErrorResponseDto;
import notificaciones.dto.PlantillaRequestDto;
import notificaciones.dto.PlantillaResponseDto;

// Documentación Swagger de API-05 a API-08; la implementación queda limpia en PlantillaController
@Tag(name = "Plantillas", description = "Administración de plantillas de mensaje (CU-07)")
public interface IPlantillaController {

    String ERROR_404 = """
            {"timestamp":"2026-10-27T19:19:11","status":404,"code":"TEMPLATE_NOT_FOUND",\
            "message":"La plantilla con id 999 no existe","path":"/api/v1/templates/999",\
            "correlationId":"a64ea3e6-5587-4a7b-bd77-005e498ae9c4"}""";

    @Operation(summary = "Crear una plantilla (CU-07)", description = """
            Crea una plantilla de un canal. Las variables del cuerpo van entre llaves, por ejemplo {pedidoId}: \
            el solicitante deberá enviarlas como parámetros. Si no se indica activa, queda activa.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plantilla creada"),
            @ApiResponse(responseCode = "400", description = "Faltan datos obligatorios, superan el largo máximo o el canal no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe una plantilla con ese nombre", content = @Content(
                    schema = @Schema(implementation = ErrorResponseDto.class), examples = @ExampleObject(
                            name = "TEMPLATE_DUPLICATED", value = "{\"status\":409,\"code\":\"TEMPLATE_DUPLICATED\",\"message\":\"Ya existe una plantilla con el nombre PEDIDO_CONFIRMADO_EMAIL\"}"))) })
    ResponseEntity<PlantillaResponseDto> crearPlantilla(PlantillaRequestDto plantillaDto);

    @Operation(summary = "Modificar una plantilla (CU-07)", description = """
            Reemplaza nombre, asunto, cuerpo y estado de la plantilla; si no se indica activa se conserva. El canal \
            no puede cambiar. Las notificaciones ya generadas conservan su mensaje.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plantilla modificada"),
            @ApiResponse(responseCode = "400", description = "Faltan datos obligatorios o superan el largo máximo",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "No existe una plantilla con ese id", content = @Content(
                    schema = @Schema(implementation = ErrorResponseDto.class), examples = @ExampleObject(
                            name = "TEMPLATE_NOT_FOUND", value = ERROR_404))),
            @ApiResponse(responseCode = "409", description = "Otra plantilla ya usa ese nombre", content = @Content(
                    schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "422", description = "Se intentó cambiar el canal (RN-10)", content = @Content(
                    schema = @Schema(implementation = ErrorResponseDto.class), examples = @ExampleObject(
                            name = "TEMPLATE_CHANNEL_IMMUTABLE", value = "{\"status\":422,\"code\":\"TEMPLATE_CHANNEL_IMMUTABLE\",\"message\":\"La plantilla 1 pertenece al canal EMAIL y no puede cambiarlo\"}"))) })
    ResponseEntity<PlantillaResponseDto> modificarPlantilla(
            @Parameter(description = "Id de la plantilla", example = "1") Long id, PlantillaRequestDto plantillaDto);

    @Operation(summary = "Consultar una plantilla (CU-07)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plantilla con sus variables"),
            @ApiResponse(responseCode = "404", description = "No existe una plantilla con ese id", content = @Content(
                    schema = @Schema(implementation = ErrorResponseDto.class), examples = @ExampleObject(
                            name = "TEMPLATE_NOT_FOUND", value = ERROR_404))) })
    ResponseEntity<PlantillaResponseDto> consultarPlantilla(
            @Parameter(description = "Id de la plantilla", example = "1") Long id);

    @Operation(summary = "Listar las plantillas (CU-07)", description = "Para que los módulos solicitantes conozcan los ids de plantilla y las variables que cada una exige.")
    @ApiResponse(responseCode = "200", description = "Todas las plantillas, activas o no")
    ResponseEntity<List<PlantillaResponseDto>> listarPlantillas();
}
