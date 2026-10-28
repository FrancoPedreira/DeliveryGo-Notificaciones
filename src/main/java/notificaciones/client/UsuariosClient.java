package notificaciones.client;

import java.util.Optional;

import notificaciones.dto.UsuarioContactoResponseDto;

// INT-01: contrato con el módulo Usuarios (GET /api/v1/users/{id}/contact, ver 13.1 del documento)
// MOMENTÁNEO: estamos a la espera de que el equipo de Usuarios publique su API. El endpoint y los campos
// de UsuarioContactoResponseDto son una propuesta nuestra; cuando la publiquen se ajusta este contrato
// y se reemplaza UsuariosClientSimulado por una implementación que llame a la API real.
public interface UsuariosClient {

    // Vacío si Usuarios responde 404. Lanza ServicioNoDisponibleException ante timeout o error 5xx
    Optional<UsuarioContactoResponseDto> obtenerContacto(Long usuarioId);
}
