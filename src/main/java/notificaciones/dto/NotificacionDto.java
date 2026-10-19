package notificaciones.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class NotificacionDto {
    private Long id;
    private Long destinatarioId;
    private Long plantillaId;
    private String canal;
    private Map<String, String> parametros;
    private String estado;
    private Integer cantidadIntentos;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaUltimoIntento;

    // Necesario para deserializar el body de la solicitud (API-01)
    public NotificacionDto() {
    }

    public NotificacionDto(Long id, Long destinatarioId, Long plantillaId, String canal,
            Map<String, String> parametros, String estado, Integer cantidadIntentos, LocalDateTime fechaCreacion,
            LocalDateTime fechaUltimoIntento) {
        this.id = id;
        this.destinatarioId = destinatarioId;
        this.plantillaId = plantillaId;
        this.canal = canal;
        this.parametros = parametros;
        this.estado = estado;
        this.cantidadIntentos = cantidadIntentos;
        this.fechaCreacion = fechaCreacion;
        this.fechaUltimoIntento = fechaUltimoIntento;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getCantidadIntentos() {
        return cantidadIntentos;
    }

    public void setCantidadIntentos(Integer cantidadIntentos) {
        this.cantidadIntentos = cantidadIntentos;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaUltimoIntento() {
        return fechaUltimoIntento;
    }

    public void setFechaUltimoIntento(LocalDateTime fechaUltimoIntento) {
        this.fechaUltimoIntento = fechaUltimoIntento;
    }
}
