package notificaciones.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
// DM-09: respaldo en la base para que un número de intento no se repita en la misma notificación
@Table(name = "intento_envio", uniqueConstraints = @UniqueConstraint(columnNames = { "notificacion_id",
        "numero_intento" }))
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

    // REL-01: un intento solo se crea a partir de su notificación, con la fecha del último intento registrado
    public static IntentoEnvio crear(Notificacion notificacion, int numeroIntento, ResultadoIntento resultado,
            String detalleError) {
        IntentoEnvio intento = new IntentoEnvio();
        intento.notificacion = notificacion;
        intento.numeroIntento = numeroIntento;
        intento.fecha = notificacion.getFechaUltimoIntento();
        intento.resultado = resultado;
        intento.detalleError = detalleError;
        return intento;
    }

    public boolean esExitoso() {
        return resultado == ResultadoIntento.EXITOSO;
    }

    // Getters
    public Long getId() {
        return id;
    }

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

    public Notificacion getNotificacion() {
        return notificacion;
    }
}
