package notificaciones.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import notificaciones.dto.NotificacionResponseDto;
import notificaciones.dto.PaginaResponseDto;
import notificaciones.services.NotificacionServices;

// La consigna fija esta ruta bajo /users aunque el recurso es de Notificaciones
@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class HistorialController implements IHistorialController {
    private final NotificacionServices notificacionServices;

    public HistorialController(NotificacionServices notificacionServices) {
        this.notificacionServices = notificacionServices;
    }

    // API-04
    @Override
    @GetMapping("/{id}/notifications")
    public ResponseEntity<PaginaResponseDto<NotificacionResponseDto>> consultarHistorial(@PathVariable Long id,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String canal,
            @RequestParam(required = false) Boolean leida,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(notificacionServices.consultarHistorial(id, estado, canal, leida, page, size, sort));
    }
}
