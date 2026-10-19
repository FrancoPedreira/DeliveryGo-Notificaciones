package notificaciones.dto;

import java.util.Map;

// DTO-01: entrada de API-01. Solo lo que el módulo solicitante puede indicar (RN-01)
public class CrearNotificacionRequestDto {
    private Long destinatarioId;
    private Long plantillaId;
    private String canal;
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
