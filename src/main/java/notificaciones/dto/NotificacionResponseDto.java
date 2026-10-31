package notificaciones.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

// DTO-02: salida de API-01 a API-03 y API-12, y elemento del historial de API-04. Nunca incluye los parámetros (RN-06);
// el mensaje solo viaja en el historial del propio destinatario (CU-06)
@Schema(description = "DTO-02: estado de una notificación. Solo el historial (CU-06) incluye el mensaje")
public class NotificacionResponseDto {
    @Schema(example = "1")
    private Long id;
    @Schema(example = "10")
    private Long destinatarioId;
    @Schema(example = "1")
    private Long plantillaId;
    @Schema(example = "EMAIL", allowableValues = { "EMAIL", "PUSH", "IN_APP" })
    private String canal;
    @Schema(description = "PENDIENTE, ENVIADA, FALLIDA (se puede reintentar) o FALLIDA_DEFINITIVA", example = "ENVIADA", allowableValues = { "PENDIENTE", "ENVIADA", "FALLIDA", "FALLIDA_DEFINITIVA" })
    private String estado;
    @Schema(description = "Intentos de envío realizados (máximo 3)", example = "1")
    private int cantidadIntentos;
    private LocalDateTime fechaCreacion;
    @Schema(description = "Ausente si todavía no hubo intentos")
    private LocalDateTime fechaUltimoIntento;
    @Schema(description = "Solo en notificaciones IN_APP leídas (CU-10)")
    private LocalDateTime fechaLectura;
    @Schema(description = "Mensaje generado; solo se informa en el historial del destinatario (CU-06)")
    private String mensaje;

    public NotificacionResponseDto(Long id, Long destinatarioId, Long plantillaId, String canal, String estado,
            int cantidadIntentos, LocalDateTime fechaCreacion, LocalDateTime fechaUltimoIntento,
            LocalDateTime fechaLectura, String mensaje) {
        this.id = id;
        this.destinatarioId = destinatarioId;
        this.plantillaId = plantillaId;
        this.canal = canal;
        this.estado = estado;
        this.cantidadIntentos = cantidadIntentos;
        this.fechaCreacion = fechaCreacion;
        this.fechaUltimoIntento = fechaUltimoIntento;
        this.fechaLectura = fechaLectura;
        this.mensaje = mensaje;
    }

    // Getters: solo lectura, Jackson los usa para armar el JSON de respuesta
    public Long getId() {
        return id;
    }

    public Long getDestinatarioId() {
        return destinatarioId;
    }

    public Long getPlantillaId() {
        return plantillaId;
    }

    public String getCanal() {
        return canal;
    }

    public String getEstado() {
        return estado;
    }

    public int getCantidadIntentos() {
        return cantidadIntentos;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaUltimoIntento() {
        return fechaUltimoIntento;
    }

    public LocalDateTime getFechaLectura() {
        return fechaLectura;
    }

    public String getMensaje() {
        return mensaje;
    }
}
