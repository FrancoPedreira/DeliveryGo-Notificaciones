package notificaciones.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import notificaciones.dto.CanalDto;
import notificaciones.dto.CanalEstadoRequestDto;
import notificaciones.services.CanalServices;

@RestController
@RequestMapping(value = "/api/v1/channels", produces = MediaType.APPLICATION_JSON_VALUE)
public class CanalController implements ICanalController {
    private final CanalServices canalServices;

    public CanalController(CanalServices canalServices) {
        this.canalServices = canalServices;
    }

    // API-09
    @Override
    @GetMapping
    public ResponseEntity<List<CanalDto>> listarCanales() {
        return ResponseEntity.ok(canalServices.listarCanales());
    }

    // API-10
    @Override
    @PatchMapping("/{tipo}")
    public ResponseEntity<CanalDto> cambiarEstadoCanal(@PathVariable String tipo,
            @RequestBody CanalEstadoRequestDto estadoDto) {
        return ResponseEntity.ok(canalServices.cambiarEstadoCanal(tipo, estadoDto.getActivo()));
    }
}
