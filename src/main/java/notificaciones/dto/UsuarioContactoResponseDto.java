package notificaciones.dto;

// DTO-04: respuesta de Usuarios en INT-01. Solo los datos mínimos de contacto (RN-06)
// MOMENTÁNEO: campos propuestos por nosotros; se ajustarán cuando el equipo de Usuarios publique su API.
public class UsuarioContactoResponseDto {
    private Long id;
    private String nombre;
    private String email;
    private String pushToken;
    private boolean activo;

    // Necesario para deserializar la respuesta de Usuarios
    public UsuarioContactoResponseDto() {
    }

    public UsuarioContactoResponseDto(Long id, String nombre, String email, String pushToken, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.pushToken = pushToken;
        this.activo = activo;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getPushToken() {
        return pushToken;
    }

    public boolean isActivo() {
        return activo;
    }
}
