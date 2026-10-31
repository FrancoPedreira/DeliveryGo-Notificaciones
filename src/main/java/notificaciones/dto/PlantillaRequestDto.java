package notificaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// DTO-05: entrada de API-05 y API-06 (CU-07)
@Schema(description = "DTO-05: alta o modificación de una plantilla")
public class PlantillaRequestDto {
    @Schema(description = "Nombre único de la plantilla", example = "PEDIDO_ENTREGADO_EMAIL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nombre;
    @Schema(description = "Asunto del mensaje", example = "Pedido entregado", requiredMode = Schema.RequiredMode.REQUIRED)
    private String asunto;
    @Schema(description = "Cuerpo con variables entre llaves, que el solicitante completa con sus parámetros", example = "Hola {nombre}, tu pedido {pedidoId} fue entregado.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String cuerpo;
    @Schema(description = "Si no se indica, la plantilla se crea activa", example = "true")
    private Boolean activa;
    @Schema(description = "Canal al que pertenece. Obligatorio al crear; al modificar no puede cambiar", example = "EMAIL", allowableValues = { "EMAIL", "PUSH", "IN_APP" })
    private String canal;

    // Necesario para deserializar el body de la solicitud
    public PlantillaRequestDto() {
    }

    public PlantillaRequestDto(String nombre, String asunto, String cuerpo, Boolean activa, String canal) {
        this.nombre = nombre;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
        this.activa = activa;
        this.canal = canal;
    }

    // Getters y Setters
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public void setCuerpo(String cuerpo) {
        this.cuerpo = cuerpo;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }
}
