package notificaciones.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

// DTO-06: salida de API-05 a API-08 (CU-07)
@Schema(description = "DTO-06: plantilla con las variables que el solicitante debe enviar como parámetros")
public class PlantillaResponseDto {
    @Schema(example = "1")
    private Long id;
    @Schema(example = "PEDIDO_CONFIRMADO_EMAIL")
    private String nombre;
    @Schema(example = "Pedido confirmado")
    private String asunto;
    @Schema(example = "Hola {nombre}, tu pedido {pedidoId} fue confirmado.")
    private String cuerpo;
    @Schema(example = "true")
    private boolean activa;
    @Schema(example = "EMAIL", allowableValues = { "EMAIL", "PUSH", "IN_APP" })
    private String canal;
    @Schema(description = "Variables del cuerpo: los parámetros que la plantilla exige (RN-08)", example = "[\"nombre\", \"pedidoId\"]")
    private List<String> variables;

    public PlantillaResponseDto(Long id, String nombre, String asunto, String cuerpo, boolean activa, String canal,
            List<String> variables) {
        this.id = id;
        this.nombre = nombre;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
        this.activa = activa;
        this.canal = canal;
        this.variables = variables;
    }

    // Getters: solo lectura, Jackson los usa para armar el JSON de respuesta
    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getAsunto() {
        return asunto;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public boolean isActiva() {
        return activa;
    }

    public String getCanal() {
        return canal;
    }

    public List<String> getVariables() {
        return variables;
    }
}
