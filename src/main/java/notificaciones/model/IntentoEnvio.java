package notificaciones.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "intentos_envio")
public class IntentoEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int numeroIntento;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultadoIntento resultado;

    // Solo el motivo técnico del fallo, nunca el contenido del mensaje (RN-06)
    private String detalleError;

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "notificacion_id", nullable = false)
    private Notificacion notificacion;

    protected IntentoEnvio() {
    }

    public IntentoEnvio(int numeroIntento, LocalDateTime fecha, ResultadoIntento resultado, String detalleError) {
        this.numeroIntento = numeroIntento;
        this.fecha = fecha;
        this.resultado = resultado;
        this.detalleError = detalleError;
    }

    // Getters y Setters
    public int getNumeroIntento() {
        return numeroIntento;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public ResultadoIntento getResultado() {
        return resultado;
    }

    public String getDetalleError() {
        return detalleError;
    }

    public void setNotificacion(Notificacion notificacion) {
        this.notificacion = notificacion;
    }
}
