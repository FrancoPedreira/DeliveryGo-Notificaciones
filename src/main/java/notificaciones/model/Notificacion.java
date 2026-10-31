package notificaciones.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.*;

@Entity
@Table(name = "notificacion")
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

    // CU-10: solo se completa en las notificaciones IN_APP que el destinatario ya leyó
    private LocalDateTime fechaLectura;

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "plantilla_id", nullable = false)
    private Plantilla plantilla;

    @ManyToOne
    @JoinColumn(name = "canal_id", nullable = false)
    private Canal canal;

    protected Notificacion() {
    }

    // Toda notificación nace PENDIENTE y sin intentos
    public Notificacion(Long destinatarioId, Canal canal) {
        this.destinatarioId = destinatarioId;
        this.canal = canal;
        this.estado = EstadoNotificacion.PENDIENTE;
        this.cantidadIntentos = 0;
        this.fechaCreacion = LocalDateTime.now();
    }

    // RN-04 y DM-08: único punto que modifica el contador y decide el estado resultante de un intento
    public void registrarIntento(ResultadoIntento resultado, int maxIntentos) {
        cantidadIntentos++;
        fechaUltimoIntento = LocalDateTime.now();
        if (resultado == ResultadoIntento.EXITOSO) {
            estado = EstadoNotificacion.ENVIADA;
        } else if (cantidadIntentos >= maxIntentos) {
            estado = EstadoNotificacion.FALLIDA_DEFINITIVA;
        } else {
            estado = EstadoNotificacion.FALLIDA;
        }
    }

    // RN-05
    public boolean puedeReintentarse() {
        return estado == EstadoNotificacion.FALLIDA;
    }

    // CU-05: vuelve a PENDIENTE para reutilizar el envío de CU-03
    public void volverAPendiente() {
        estado = EstadoNotificacion.PENDIENTE;
    }

    // RN-12 (CU-10): solo una notificación IN_APP ya entregada puede marcarse como leída
    public boolean puedeMarcarseComoLeida() {
        return canal.getTipo() == TipoCanal.IN_APP && estado == EstadoNotificacion.ENVIADA;
    }

    // RN-12: marcar es idempotente, conserva la fecha de la primera lectura
    public void marcarComoLeida() {
        if (fechaLectura == null) {
            // Milisegundos: la base no guarda los nanosegundos y la respuesta debe coincidir con lo persistido
            fechaLectura = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        }
    }

    public boolean estaLeida() {
        return fechaLectura != null;
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

    public LocalDateTime getFechaLectura() {
        return fechaLectura;
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

    public void setPlantilla(Plantilla plantilla) {
        this.plantilla = plantilla;
    }
}
