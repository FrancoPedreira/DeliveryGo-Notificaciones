package notificaciones.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import notificaciones.dto.CrearNotificacionRequestDto;
import notificaciones.dto.IntentoEnvioResponseDto;
import notificaciones.dto.NotificacionResponseDto;
import notificaciones.services.NotificacionServices;

@RestController
@RequestMapping(value = "/api/v1/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
public class NotificacionController implements INotificacionController {
    private final NotificacionServices notificacionServices;

    public NotificacionController(NotificacionServices notificacionServices) {
        this.notificacionServices = notificacionServices;
    }

    // API-01
    @Override
    @PostMapping
    public ResponseEntity<NotificacionResponseDto> crearNotificacion(
            @RequestBody CrearNotificacionRequestDto notificacionDto) {
        // Los errores los traduce a DTO-03 el GlobalExceptionHandler
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificacionServices.crearNotificacion(notificacionDto));
    }

    // API-02
    @Override
    @GetMapping("/{id}")
    public ResponseEntity<NotificacionResponseDto> consultarEstado(@PathVariable Long id) {
        return ResponseEntity.ok(notificacionServices.consultarEstado(id));
    }

    // API-03
    @Override
    @PostMapping("/{id}/retry")
    public ResponseEntity<NotificacionResponseDto> reintentarNotificacion(@PathVariable Long id) {
        return ResponseEntity.ok(notificacionServices.reintentarNotificacion(id));
    }

    // API-11
    @Override
    @GetMapping("/{id}/attempts")
    public ResponseEntity<List<IntentoEnvioResponseDto>> consultarIntentos(@PathVariable Long id) {
        return ResponseEntity.ok(notificacionServices.consultarIntentos(id));
    }

    // API-12
    @Override
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificacionResponseDto> marcarComoLeida(@PathVariable Long id) {
        return ResponseEntity.ok(notificacionServices.marcarComoLeida(id));
    }
}
