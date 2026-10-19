package notificaciones.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import notificaciones.dto.CrearNotificacionRequestDto;
import notificaciones.dto.NotificacionResponseDto;
import notificaciones.services.NotificacionServices;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificacionController {
    private final NotificacionServices notificacionServices;

    public NotificacionController(NotificacionServices notificacionServices) {
        this.notificacionServices = notificacionServices;
    }

    // API-01
    @PostMapping
    public ResponseEntity<NotificacionResponseDto> crearNotificacion(
            @RequestBody CrearNotificacionRequestDto notificacionDto) {
        // Los errores los traduce a DTO-03 el GlobalExceptionHandler
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificacionServices.crearNotificacion(notificacionDto));
    }

    // API-02
    @GetMapping("/{id}")
    public ResponseEntity<NotificacionResponseDto> consultarEstado(@PathVariable Long id) {
        return ResponseEntity.ok(notificacionServices.consultarEstado(id));
    }

    // API-03
    @PostMapping("/{id}/retry")
    public ResponseEntity<NotificacionResponseDto> reintentarNotificacion(@PathVariable Long id) {
        return ResponseEntity.ok(notificacionServices.reintentarNotificacion(id));
    }
}
