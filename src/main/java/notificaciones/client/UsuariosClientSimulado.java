package notificaciones.client;

import java.util.Map;
import java.util.Optional;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import notificaciones.dto.UsuarioContactoResponseDto;
import notificaciones.exception.ServicioNoDisponibleException;

// DM-10: reemplaza a Usuarios hasta integrar el módulo real; se cambia por un cliente HTTP sin tocar los services
// MOMENTÁNEO: implementación simulada mientras esperamos que el equipo de Usuarios publique su API.
// Cuando esté disponible se modificará: se crea el cliente real y esta clase se elimina o queda solo para tests.
// Es la implementación por defecto: notificaciones.usuarios.modo=simulado (o sin definir)
@Component
@ConditionalOnProperty(name = "notificaciones.usuarios.modo", havingValue = "simulado", matchIfMissing = true)
public class UsuariosClientSimulado implements UsuariosClient {
    private static final Map<Long, UsuarioContactoResponseDto> USUARIOS = Map.of(
            10L, new UsuarioContactoResponseDto(10L, "Ana", "ana@mail.com", "push-token-ana", true),
            11L, new UsuarioContactoResponseDto(11L, "Bruno", "bruno@mail.com", null, true),
            12L, new UsuarioContactoResponseDto(12L, "Carla", "carla@mail.com", "push-token-carla", false));

    private final boolean simularCaida;

    public UsuariosClientSimulado(@Value("${notificaciones.usuarios.simular-caida:false}") boolean simularCaida) {
        this.simularCaida = simularCaida;
        // Se avisa al arrancar para que nunca se confunda con la integración real
        LoggerFactory.getLogger(UsuariosClientSimulado.class).warn("INT-01 SIMULADA: no se consulta al módulo "
                + "Usuarios; se usan los usuarios de prueba 10, 11 y 12. Para la integración real: "
                + "notificaciones.usuarios.modo=http");
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
