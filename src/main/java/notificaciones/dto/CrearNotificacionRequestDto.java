package notificaciones.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

// DTO-01: entrada de API-01. Solo lo que el módulo solicitante puede indicar (RN-01)
@Schema(description = "DTO-01: solicitud de notificación del contrato común")
public class CrearNotificacionRequestDto {
    @Schema(description = "Id del usuario destinatario (módulo Usuarios)", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long destinatarioId;
    @Schema(description = "Id de la plantilla; define el tipo de notificación y el canal al que pertenece", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long plantillaId;
    @Schema(description = "Canal de envío; debe coincidir con el canal de la plantilla", example = "EMAIL", allowableValues = { "EMAIL", "PUSH", "IN_APP" }, requiredMode = Schema.RequiredMode.REQUIRED)
    private String canal;
    @Schema(description = "Valores para las variables de la plantilla; los que la plantilla no usa se descartan", example = "{\"nombre\": \"Ana\", \"pedidoId\": \"55\"}")
    private Map<String, String> parametros;

    // Necesario para deserializar el body de la solicitud
    public CrearNotificacionRequestDto() {
    }

    public CrearNotificacionRequestDto(Long destinatarioId, Long plantillaId, String canal,
            Map<String, String> parametros) {
        this.destinatarioId = destinatarioId;
        this.plantillaId = plantillaId;
        this.canal = canal;
        this.parametros = parametros;
    }

    // Getters y Setters
    public Long getDestinatarioId() {
        return destinatarioId;
    }

    public void setDestinatarioId(Long destinatarioId) {
        this.destinatarioId = destinatarioId;
    }

    public Long getPlantillaId() {
        return plantillaId;
    }

    public void setPlantillaId(Long plantillaId) {
        this.plantillaId = plantillaId;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public Map<String, String> getParametros() {
        return parametros;
    }

    public void setParametros(Map<String, String> parametros) {
        this.parametros = parametros;
    }
}
