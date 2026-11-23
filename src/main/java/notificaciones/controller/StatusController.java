package notificaciones.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Chequeo de disponibilidad del módulo. Público: no requiere JWT.
// Informa también si INT-01 (Usuarios) está simulada o es real, para que no se confundan en la demo
@RestController
public class StatusController {
    private final String modoUsuarios;

    public StatusController(@Value("${notificaciones.usuarios.modo:simulado}") String modoUsuarios) {
        this.modoUsuarios = modoUsuarios;
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of("estado", "UP", "modulo", "notificaciones",
                "integracionUsuarios", modoUsuarios));
    }
}
