package notificaciones.services;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import notificaciones.dto.PlantillaDto;
import notificaciones.exception.ReglaNegocioException;
import notificaciones.exception.ValidacionException;
import notificaciones.model.Canal;
import notificaciones.model.Notificacion;
import notificaciones.model.Plantilla;
import notificaciones.repository.PlantillaRepository;
import static notificaciones.utils.Validaciones.*;

@Service
public class PlantillaServices {
    private final PlantillaRepository plantillaRepository;
    private final CanalServices canalServices;

    public PlantillaServices(PlantillaRepository plantillaRepository, CanalServices canalServices) {
        this.plantillaRepository = plantillaRepository;
        this.canalServices = canalServices;
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

        Canal canal = canalServices.obtenerCanalPorTipo(plantillaDto.getCanal().getTipo());

        Plantilla plantilla = new Plantilla(plantillaDto.getNombre(), plantillaDto.getAsunto(),
                plantillaDto.getCuerpo(), plantillaDto.isActiva(), canal);
        plantillaRepository.save(plantilla);
        return plantillaDto;
    }

    // SRV-02 (CU-02). DM-09: participa de la transacción de SRV-01, nunca abre una propia
    @Transactional(propagation = Propagation.MANDATORY)
    public void generarMensaje(Notificacion notificacion, Long plantillaId, Map<String, String> parametros) {
        // RN-02 (REP-02)
        Plantilla plantilla = plantillaRepository.findById(plantillaId).orElse(null);
        if (plantilla == null || !plantilla.isActiva() || !plantilla.admiteCanal(notificacion.getCanal())) {
            throw new ReglaNegocioException("TEMPLATE_INVALID", "La plantilla " + plantillaId
                    + " no existe, está inactiva o pertenece a otro canal");
        }

        // RN-08 y RN-06: si faltan parámetros lanza TEMPLATE_PARAMS_MISSING
        notificacion.setPlantilla(plantilla);
        notificacion.setMensaje(plantilla.renderizar(parametros));
    }
}
