package notificaciones.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import notificaciones.dto.PlantillaRequestDto;
import notificaciones.dto.PlantillaResponseDto;
import notificaciones.services.PlantillaServices;

@RestController
@RequestMapping(value = "/api/v1/templates", produces = MediaType.APPLICATION_JSON_VALUE)
public class PlantillaController implements IPlantillaController {
    private final PlantillaServices plantillaServices;

    public PlantillaController(PlantillaServices plantillaServices) {
        this.plantillaServices = plantillaServices;
    }

    // API-05
    @Override
    @PostMapping
    public ResponseEntity<PlantillaResponseDto> crearPlantilla(@RequestBody PlantillaRequestDto plantillaDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plantillaServices.crearPlantilla(plantillaDto));
    }

    // API-06
    @Override
    @PutMapping("/{id}")
    public ResponseEntity<PlantillaResponseDto> modificarPlantilla(@PathVariable Long id,
            @RequestBody PlantillaRequestDto plantillaDto) {
        return ResponseEntity.ok(plantillaServices.modificarPlantilla(id, plantillaDto));
    }

    // API-07
    @Override
    @GetMapping("/{id}")
    public ResponseEntity<PlantillaResponseDto> consultarPlantilla(@PathVariable Long id) {
        return ResponseEntity.ok(plantillaServices.consultarPlantilla(id));
    }

    // API-08
    @Override
    @GetMapping
    public ResponseEntity<List<PlantillaResponseDto>> listarPlantillas() {
        return ResponseEntity.ok(plantillaServices.listarPlantillas());
    }
}
