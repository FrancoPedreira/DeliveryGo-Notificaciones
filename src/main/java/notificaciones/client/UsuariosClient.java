package notificaciones.client;

import java.util.Optional;

import notificaciones.dto.UsuarioContactoResponseDto;

// INT-01: contrato con el módulo Usuarios (GET /api/v1/users/{id}/contact, ver 13.1 del documento)
public interface UsuariosClient {

    // Vacío si Usuarios responde 404. Lanza ServicioNoDisponibleException ante timeout o error 5xx
    Optional<UsuarioContactoResponseDto> obtenerContacto(Long usuarioId);
}
