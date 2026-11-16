package notificaciones.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// DTO-04: datos de contacto que Notificaciones necesita para INT-01.
// Usuarios informa el estado como texto; Notificaciones lo convierte en el booleano activo.
@JsonIgnoreProperties(ignoreUnknown = true)
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

    // Usuarios devuelve "estado"; Notificaciones usa el booleano "activo".
    public void setEstado(String estado) {
        this.activo = "ACTIVO".equals(estado);
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
