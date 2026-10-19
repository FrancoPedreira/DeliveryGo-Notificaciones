package notificaciones.dto;

import java.time.LocalDateTime;

public class IntentoEnvioDto {
    private int numeroIntento;
    private LocalDateTime fecha;
    private String resultado;
    private String detalleError;

    public IntentoEnvioDto(int numeroIntento, LocalDateTime fecha, String resultado, String detalleError) {
        this.numeroIntento = numeroIntento;
        this.fecha = fecha;
        this.resultado = resultado;
        this.detalleError = detalleError;
    }

    // Getters y Setters
    public int getNumeroIntento() {
        return numeroIntento;
    }

    public void setNumeroIntento(int numeroIntento) {
        this.numeroIntento = numeroIntento;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getDetalleError() {
        return detalleError;
    }

    public void setDetalleError(String detalleError) {
        this.detalleError = detalleError;
    }
}
