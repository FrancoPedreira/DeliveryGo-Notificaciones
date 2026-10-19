package notificaciones.dto;

import java.time.LocalDateTime;

// DTO-02: salida de API-01 a API-03. No incluye mensaje ni parámetros para no exponer datos sensibles (RN-06)
public class NotificacionResponseDto {
    private Long id;
    private Long destinatarioId;
    private Long plantillaId;
    private String canal;
    private String estado;
    private int cantidadIntentos;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaUltimoIntento;

    public NotificacionResponseDto(Long id, Long destinatarioId, Long plantillaId, String canal, String estado,
            int cantidadIntentos, LocalDateTime fechaCreacion, LocalDateTime fechaUltimoIntento) {
        this.id = id;
        this.destinatarioId = destinatarioId;
        this.plantillaId = plantillaId;
        this.canal = canal;
        this.estado = estado;
        this.cantidadIntentos = cantidadIntentos;
        this.fechaCreacion = fechaCreacion;
        this.fechaUltimoIntento = fechaUltimoIntento;
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
}
