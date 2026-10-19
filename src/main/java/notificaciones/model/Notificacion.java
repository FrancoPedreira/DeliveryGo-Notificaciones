package notificaciones.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // No es FK: referencia a un usuario del módulo Usuarios (se valida vía INT-01)
    @Column(nullable = false)
    private Long destinatarioId;

    @Column(length = 1000)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoNotificacion estado;

    @Column(nullable = false)
    private int cantidadIntentos;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaUltimoIntento;

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "plantilla_id", nullable = false)
    private Plantilla plantilla;

    @ManyToOne
    @JoinColumn(name = "canal_id", nullable = false)
    private Canal canal;

    protected Notificacion() {
    }

    public Notificacion(Long destinatarioId, LocalDateTime fechaCreacion, EstadoNotificacion estado) {
        this.destinatarioId = destinatarioId;
        this.fechaCreacion = fechaCreacion;
        this.estado = estado;
        this.cantidadIntentos = 0;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public Long getDestinatarioId() {
        return destinatarioId;
    }

    public String getMensaje() {
        return mensaje;
    }

    public EstadoNotificacion getEstado() {
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

    public Plantilla getPlantilla() {
        return plantilla;
    }

    public Canal getCanal() {
        return canal;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public void setEstado(EstadoNotificacion estado) {
        this.estado = estado;
    }

    public void setCantidadIntentos(int cantidadIntentos) {
        this.cantidadIntentos = cantidadIntentos;
    }

    public void setFechaUltimoIntento(LocalDateTime fechaUltimoIntento) {
        this.fechaUltimoIntento = fechaUltimoIntento;
    }

    public void setPlantilla(Plantilla plantilla) {
        this.plantilla = plantilla;
    }

    public void setCanal(Canal canal) {
        this.canal = canal;
    }
}
