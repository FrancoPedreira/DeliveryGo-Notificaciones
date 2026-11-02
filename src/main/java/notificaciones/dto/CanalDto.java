package notificaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// DTO-07: salida de API-09 y API-10 (CU-08)
@Schema(description = "DTO-07: canal de envío y si está habilitado")
public class CanalDto {
    @Schema(description = "Tipo de canal", example = "EMAIL", allowableValues = { "EMAIL", "PUSH", "IN_APP" })
    private String tipo;
    @Schema(description = "Nombre descriptivo del canal", example = "Correo electrónico")
    private String nombre;
    @Schema(description = "true si el canal acepta notificaciones nuevas (RN-07)", example = "true")
    private boolean activo;

    public CanalDto(String tipo, String nombre, boolean activo) {
        this.tipo = tipo;
        this.nombre = nombre;
        this.activo = activo;
    }

    // Getters y Setters
    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
