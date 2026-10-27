package notificaciones.model;

import jakarta.persistence.*;

@Entity
@Table(name = "plantilla")
public class Plantilla {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private String asunto;

    // Cuerpo con variables entre llaves, ej: "Hola {nombre}, tu pedido {pedidoId} fue confirmado"
    @Column(nullable = false, length = 1000)
    private String cuerpo;

    @Column(nullable = false)
    private boolean activa;

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "canal_id", nullable = false)
    private Canal canal;

    protected Plantilla() {
    }

    public Plantilla(String nombre, String asunto, String cuerpo, boolean activa, Canal canal) {
        this.nombre = nombre;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
        this.activa = activa;
        this.canal = canal;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getAsunto() {
        return asunto;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public boolean isActiva() {
        return activa;
    }

    public Canal getCanal() {
        return canal;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
