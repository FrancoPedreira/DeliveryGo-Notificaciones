package notificaciones.services;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import notificaciones.dto.PlantillaDto;
import notificaciones.exception.ValidacionException;
import notificaciones.model.Canal;
import notificaciones.model.Plantilla;
import notificaciones.model.TipoCanal;
import notificaciones.repository.CanalRepository;
import notificaciones.repository.PlantillaRepository;
import static notificaciones.utils.Validaciones.*;

@Service
public class PlantillaServices {
    private final PlantillaRepository plantillaRepository;
    private final CanalRepository canalRepository;

    public PlantillaServices(PlantillaRepository plantillaRepository, CanalRepository canalRepository) {
        this.plantillaRepository = plantillaRepository;
        this.canalRepository = canalRepository;
    }

    private PlantillaDto validarPlantilla(PlantillaDto plantillaDto) {
        validateValidCharacters(plantillaDto.getNombre(), "nombre");
        validateRequiredText(plantillaDto.getAsunto(), "asunto");
        validateRequiredText(plantillaDto.getCuerpo(), "cuerpo");
        if (plantillaDto.getCanal() == null) {
            throw new ValidacionException("El canal no puede ser nulo");
        }
        validateRequiredText(plantillaDto.getCanal().getTipo(), "canal");
        return plantillaDto;
    }

    @Transactional
    public PlantillaDto crearPlantilla(PlantillaDto plantillaDto) {
        validarPlantilla(plantillaDto);

        Canal canal;
        try {
            canal = canalRepository
                    .findByTipo(TipoCanal.valueOf(plantillaDto.getCanal().getTipo().trim().toUpperCase()))
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            canal = null;
        }
        if (canal == null) {
            throw new ValidacionException("El canal " + plantillaDto.getCanal().getTipo() + " no existe.");
        }

        Plantilla plantilla = new Plantilla(plantillaDto.getNombre(), plantillaDto.getAsunto(),
                plantillaDto.getCuerpo(), plantillaDto.isActiva(), canal);
        plantillaRepository.save(plantilla);
        return plantillaDto;
    }
}
