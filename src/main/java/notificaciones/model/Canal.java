package notificaciones.model;

import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "canales")
public class Canal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private TipoCanal tipo;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private boolean activo;

    // Relaciones
    @OneToMany(mappedBy = "canal", fetch = FetchType.LAZY)
    private List<Plantilla> plantillas;

    @OneToMany(mappedBy = "canal", fetch = FetchType.LAZY)
    private List<Notificacion> notificaciones;

    protected Canal() {
    }

    public Canal(TipoCanal tipo, String nombre, boolean activo) {
        this.tipo = tipo;
        this.nombre = nombre;
        this.activo = activo;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public TipoCanal getTipo() {
        return tipo;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
