package notificaciones.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Chequeo de disponibilidad del módulo. Público: no requiere JWT.
@RestController
public class StatusController {

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of("estado", "UP", "modulo", "notificaciones"));
    }
}
