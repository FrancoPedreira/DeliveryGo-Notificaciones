package notificaciones.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import notificaciones.dto.CanalDto;
import notificaciones.exception.RecursoNoEncontradoException;
import notificaciones.exception.ReglaNegocioException;
import notificaciones.exception.ValidacionException;
import notificaciones.model.Canal;
import notificaciones.model.TipoCanal;
import notificaciones.repository.CanalRepository;
import static notificaciones.utils.Validaciones.validateRequiredText;

@Service
public class CanalServices {
    private final CanalRepository canalRepository;

    public CanalServices(CanalRepository canalRepository) {
        this.canalRepository = canalRepository;
    }

    // Único lugar que convierte el texto recibido ("email", "PUSH"...) en un canal de la base.
    // Vacío si el texto no es un TipoCanal o el canal no está cargado; cada caso de uso decide qué error devolver
    private Optional<Canal> buscarCanal(String nombreCanal) {
        if (nombreCanal == null) {
            return Optional.empty();
        }
        try {
            return canalRepository.findByTipo(TipoCanal.valueOf(nombreCanal.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private CanalDto toDto(Canal canal) {
        return new CanalDto(canal.getTipo().name(), canal.getNombre(), canal.estaHabilitado());
    }

    // SRV-08 (CU-08, REP-11): canales disponibles y si están habilitados
    @Transactional(readOnly = true)
    public List<CanalDto> listarCanales() {
        return canalRepository.findAll().stream().map(this::toDto).toList();
    }

    // SRV-08 (CU-08, RN-07): habilitar o deshabilitar un canal sin tocar código. Idempotente.
    // Las notificaciones ya registradas no cambian: si el canal está inactivo al enviarlas, el intento falla (CU-03 A3)
    @Transactional
    public CanalDto cambiarEstadoCanal(String tipoCanal, Boolean activo) {
        validateRequiredText(tipoCanal, "tipo");
        if (activo == null) {
            throw new ValidacionException("El campo activo no puede ser nulo");
        }
        // CU-08 A4: el canal a administrar no existe → 404
        Canal canal = buscarCanal(tipoCanal).orElseThrow(() -> new RecursoNoEncontradoException("CHANNEL_NOT_FOUND",
                "El canal " + tipoCanal + " no existe"));

        if (activo) {
            canal.activar();
        } else {
            canal.desactivar();
        }
        return toDto(canalRepository.save(canal));
    }

    // CU-07 A1: el canal de una plantilla no existe → 400
    public Canal obtenerCanalPorTipo(String nombreCanal) {
        return buscarCanal(nombreCanal)
                .orElseThrow(() -> new ValidacionException("El canal " + nombreCanal + " no existe."));
    }

    // RN-07 (REP-06), CU-01 A2: el canal solicitado debe existir y estar activo → si no, 422
    @Transactional(readOnly = true)
    public Canal obtenerCanalActivo(String nombreCanal) {
        return buscarCanal(nombreCanal)
                .filter(Canal::estaHabilitado)
                .orElseThrow(() -> new ReglaNegocioException("CHANNEL_INACTIVE",
                        "El canal " + nombreCanal + " no existe o está inactivo"));
    }
}
