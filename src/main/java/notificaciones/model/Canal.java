package notificaciones.model;

import jakarta.persistence.*;

@Entity
@Table(name = "canal")
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
