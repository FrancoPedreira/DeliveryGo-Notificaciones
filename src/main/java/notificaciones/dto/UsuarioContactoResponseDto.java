package notificaciones.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    // Necesario para deserializar la respuesta de Usuarios. @JsonCreator evita que Jackson 3 elija el
    // constructor de 5 parámetros: Usuarios no envía "activo" y fallaría al mapear null en un boolean.
    @JsonCreator
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

    // Setters para deserializar la respuesta JSON
    public void setId(Long id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
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
