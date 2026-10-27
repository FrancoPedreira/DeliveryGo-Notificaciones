package notificaciones.client;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import notificaciones.dto.UsuarioContactoResponseDto;
import notificaciones.exception.ServicioNoDisponibleException;

// DM-10: reemplaza a Usuarios hasta integrar el módulo real; se cambia por un cliente HTTP sin tocar los services
@Component
public class UsuariosClientSimulado implements UsuariosClient {
    private static final Map<Long, UsuarioContactoResponseDto> USUARIOS = Map.of(
            10L, new UsuarioContactoResponseDto(10L, "Ana", "ana@mail.com", "push-token-ana", true),
            11L, new UsuarioContactoResponseDto(11L, "Bruno", "bruno@mail.com", null, true),
            12L, new UsuarioContactoResponseDto(12L, "Carla", "carla@mail.com", "push-token-carla", false));

    private final boolean simularCaida;

    public UsuariosClientSimulado(@Value("${notificaciones.usuarios.simular-caida:false}") boolean simularCaida) {
        this.simularCaida = simularCaida;
    }

    @Override
    public Optional<UsuarioContactoResponseDto> obtenerContacto(Long usuarioId) {
        if (simularCaida) {
            throw new ServicioNoDisponibleException("USERS_SERVICE_UNAVAILABLE",
                    "El módulo Usuarios no está disponible");
        }
        return Optional.ofNullable(USUARIOS.get(usuarioId));
    }
}
