package notificaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// DTO-08: entrada de API-10 (CU-08)
@Schema(description = "DTO-08: habilita o deshabilita un canal")
public class CanalEstadoRequestDto {
    @Schema(description = "true habilita el canal, false lo deshabilita", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean activo;

    // Necesario para deserializar el body de la solicitud
    public CanalEstadoRequestDto() {
    }

    public CanalEstadoRequestDto(Boolean activo) {
        this.activo = activo;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
