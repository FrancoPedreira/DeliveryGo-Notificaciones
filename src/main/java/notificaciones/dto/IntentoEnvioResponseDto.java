package notificaciones.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

// DTO-09: salida de API-11 (CU-09). Solo el motivo técnico del fallo, nunca el mensaje (RN-06)
@Schema(description = "DTO-09: un intento de envío de una notificación")
public class IntentoEnvioResponseDto {
    @Schema(example = "1")
    private int numeroIntento;
    private LocalDateTime fecha;
    @Schema(example = "FALLIDO", allowableValues = { "EXITOSO", "FALLIDO" })
    private String resultado;
    @Schema(description = "Motivo del fallo; ausente si el intento fue exitoso", example = "Fallo simulado del proveedor EMAIL")
    private String detalleError;

    public IntentoEnvioResponseDto(int numeroIntento, LocalDateTime fecha, String resultado, String detalleError) {
        this.numeroIntento = numeroIntento;
        this.fecha = fecha;
        this.resultado = resultado;
        this.detalleError = detalleError;
    }

    // Getters: solo lectura, Jackson los usa para armar el JSON de respuesta
    public int getNumeroIntento() {
        return numeroIntento;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getResultado() {
        return resultado;
    }

    public String getDetalleError() {
        return detalleError;
    }
}
