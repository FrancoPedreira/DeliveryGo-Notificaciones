package notificaciones.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO-03: formato común de error de todas las APIs")
public class ErrorResponseDto {
    private LocalDateTime timestamp;
    @Schema(description = "Código HTTP", example = "422")
    private int status;
    @Schema(description = "Código de error para que el llamador decida qué hacer", example = "TEMPLATE_PARAMS_MISSING")
    private String code;
    @Schema(description = "Detalle legible del error", example = "Faltan los parámetros pedidoId")
    private String message;
    @Schema(example = "/api/v1/notifications")
    private String path;
    @Schema(description = "Id para seguir la solicitud entre módulos; se reutiliza el header X-Correlation-Id si viene", example = "f42e7e74-c72a-4423-b931-0a2828508aee")
    private String correlationId;

    public ErrorResponseDto(LocalDateTime timestamp, int status, String code, String message, String path,
            String correlationId) {
        this.timestamp = timestamp;
        this.status = status;
        this.code = code;
        this.message = message;
        this.path = path;
        this.correlationId = correlationId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getPath() {
        return path;
    }

    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }


    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
