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

    // RN-07
    public boolean estaHabilitado() {
        return activo;
    }

    public void desactivar() {
        this.activo = false;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public TipoCanal getTipo() {
        return tipo;
    }

    public String getNombre() {
        return nombre;
    }
}
