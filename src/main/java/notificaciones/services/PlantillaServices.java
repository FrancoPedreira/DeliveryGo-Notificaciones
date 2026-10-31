package notificaciones.services;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import notificaciones.dto.PlantillaRequestDto;
import notificaciones.dto.PlantillaResponseDto;
import notificaciones.exception.RecursoDuplicadoException;
import notificaciones.exception.RecursoNoEncontradoException;
import notificaciones.exception.ReglaNegocioException;
import notificaciones.model.Canal;
import notificaciones.model.Notificacion;
import notificaciones.model.Plantilla;
import notificaciones.model.TipoCanal;
import notificaciones.repository.PlantillaRepository;
import static notificaciones.utils.Validaciones.*;

@Service
public class PlantillaServices {
    private static final int MAX_NOMBRE = 255;
    private static final int MAX_ASUNTO = 255;
    private static final int MAX_CUERPO = 1000;

    private final PlantillaRepository plantillaRepository;
    private final CanalServices canalServices;

    public PlantillaServices(PlantillaRepository plantillaRepository, CanalServices canalServices) {
        this.plantillaRepository = plantillaRepository;
        this.canalServices = canalServices;
    }

    // RN-10: datos obligatorios y límites de largo de la plantilla
    private PlantillaRequestDto validarPlantilla(PlantillaRequestDto plantillaDto) {
        validateValidCharacters(plantillaDto.getNombre(), "nombre");
        validateMaxLength(plantillaDto.getNombre(), "nombre", MAX_NOMBRE);
        validateRequiredText(plantillaDto.getAsunto(), "asunto");
        validateMaxLength(plantillaDto.getAsunto(), "asunto", MAX_ASUNTO);
        validateRequiredText(plantillaDto.getCuerpo(), "cuerpo");
        validateMaxLength(plantillaDto.getCuerpo(), "cuerpo", MAX_CUERPO);
        return plantillaDto;
    }

    private Plantilla obtenerPlantilla(Long id) {
        return plantillaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("TEMPLATE_NOT_FOUND",
                        "La plantilla con id " + id + " no existe"));
    }

    // Entidad → DTO-06: la entidad no sale del service
    private PlantillaResponseDto toDto(Plantilla plantilla) {
        return new PlantillaResponseDto(plantilla.getId(), plantilla.getNombre(), plantilla.getAsunto(),
                plantilla.getCuerpo(), plantilla.isActiva(), plantilla.getCanal().getTipo().name(),
                plantilla.variables());
    }

    // SRV-07 (CU-07): alta. Se usa también desde el DataLoader para cargar las plantillas iniciales
    @Transactional
    public PlantillaResponseDto crearPlantilla(PlantillaRequestDto plantillaDto) {
        validarPlantilla(plantillaDto);
        validateRequiredText(plantillaDto.getCanal(), "canal");

        // RN-10 (REP-09): el nombre identifica a la plantilla, no puede repetirse
        if (plantillaRepository.existsByNombre(plantillaDto.getNombre())) {
            throw new RecursoDuplicadoException("TEMPLATE_DUPLICATED",
                    "Ya existe una plantilla con el nombre " + plantillaDto.getNombre());
        }

        Canal canal = canalServices.obtenerCanalPorTipo(plantillaDto.getCanal());
        boolean activa = plantillaDto.getActiva() == null || plantillaDto.getActiva();

        Plantilla plantilla = new Plantilla(plantillaDto.getNombre(), plantillaDto.getAsunto(),
                plantillaDto.getCuerpo(), activa, canal);
        return toDto(plantillaRepository.save(plantilla));
    }

    // SRV-07 (CU-07): modificación. RN-10: el canal no cambia y los mensajes ya generados no se alteran
    @Transactional
    public PlantillaResponseDto modificarPlantilla(Long id, PlantillaRequestDto plantillaDto) {
        validateId(id, "id");
        validarPlantilla(plantillaDto);
        Plantilla plantilla = obtenerPlantilla(id);

        if (plantillaDto.getCanal() != null && !plantillaDto.getCanal().isBlank()) {
            TipoCanal pedido = canalServices.obtenerCanalPorTipo(plantillaDto.getCanal()).getTipo();
            if (pedido != plantilla.getCanal().getTipo()) {
                throw new ReglaNegocioException("TEMPLATE_CHANNEL_IMMUTABLE", "La plantilla " + id
                        + " pertenece al canal " + plantilla.getCanal().getTipo() + " y no puede cambiarlo");
            }
        }
        if (plantillaRepository.existsByNombreAndIdNot(plantillaDto.getNombre(), id)) {
            throw new RecursoDuplicadoException("TEMPLATE_DUPLICATED",
                    "Ya existe una plantilla con el nombre " + plantillaDto.getNombre());
        }

        boolean activa = plantillaDto.getActiva() == null ? plantilla.isActiva() : plantillaDto.getActiva();
        plantilla.modificar(plantillaDto.getNombre(), plantillaDto.getAsunto(), plantillaDto.getCuerpo(), activa);
        return toDto(plantillaRepository.save(plantilla));
    }

    // SRV-07 (CU-07): consulta
    @Transactional(readOnly = true)
    public PlantillaResponseDto consultarPlantilla(Long id) {
        validateId(id, "id");
        return toDto(obtenerPlantilla(id));
    }

    // SRV-07 (CU-07): REP-10, para que los solicitantes conozcan los ids y las variables disponibles
    @Transactional(readOnly = true)
    public List<PlantillaResponseDto> listarPlantillas() {
        return plantillaRepository.findAll().stream().map(this::toDto).toList();
    }

    // RN-13 (CU-08): canal de la plantilla cuando el solicitante no indica uno. Si la plantilla no existe es RN-02
    @Transactional(readOnly = true)
    public String canalDePlantilla(Long plantillaId) {
        Plantilla plantilla = plantillaRepository.findById(plantillaId).orElse(null);
        if (plantilla == null) {
            throw new ReglaNegocioException("TEMPLATE_INVALID", "La plantilla " + plantillaId + " no existe");
        }
        return plantilla.getCanal().getTipo().name();
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
