package notificaciones.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import notificaciones.dto.CanalDto;
import notificaciones.exception.RecursoDuplicadoException;
import notificaciones.exception.RecursoNoEncontradoException;
import notificaciones.exception.ReglaNegocioException;
import notificaciones.exception.ValidacionException;
import notificaciones.model.Canal;
import notificaciones.model.TipoCanal;
import notificaciones.repository.CanalRepository;
import static notificaciones.utils.Validaciones.*;

@Service
public class CanalServices {
    private final CanalRepository canalRepository;

    public CanalServices(CanalRepository canalRepository) {
        this.canalRepository = canalRepository;
    }

    private CanalDto validarCanal(CanalDto canalDto) {
        validateRequiredText(canalDto.getTipo(), "tipo");
        validateValidCharacters(canalDto.getNombre(), "nombre");
        return canalDto;
    }

    @Transactional
    public CanalDto crearCanal(CanalDto canalDto) {
        validarCanal(canalDto);

        TipoCanal tipo;
        try {
            tipo = TipoCanal.valueOf(canalDto.getTipo().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidacionException("El tipo de canal " + canalDto.getTipo() + " no existe");
        }
        if (canalRepository.findByTipo(tipo).isPresent()) {
            throw new RecursoDuplicadoException("CHANNEL_DUPLICATED", "El canal " + tipo + " ya existe");
        }

        Canal canal = new Canal(tipo, canalDto.getNombre(), canalDto.isActivo());
        canalRepository.save(canal);
        return canalDto;
    }

    private CanalDto toDto(Canal canal) {
        return new CanalDto(canal.getTipo().name(), canal.getNombre(), canal.isActivo());
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
        Canal canal;
        try {
            canal = canalRepository.findByTipo(TipoCanal.valueOf(tipoCanal.trim().toUpperCase())).orElse(null);
        } catch (IllegalArgumentException e) {
            canal = null;
        }
        if (canal == null) {
            throw new RecursoNoEncontradoException("CHANNEL_NOT_FOUND", "El canal " + tipoCanal + " no existe");
        }

        if (activo) {
            canal.activar();
        } else {
            canal.desactivar();
        }
        return toDto(canalRepository.save(canal));
    }

    public Canal obtenerCanalPorTipo(String nombreCanal) {
        try {
            return canalRepository.findByTipo(TipoCanal.valueOf(nombreCanal.trim().toUpperCase()))
                    .orElseThrow(() -> new ValidacionException("El canal " + nombreCanal + " no existe."));
        } catch (IllegalArgumentException e) {
            throw new ValidacionException("El canal " + nombreCanal + " no existe.");
        }
    }

    // RN-07 (REP-06): el canal solicitado debe existir y estar activo
    @Transactional(readOnly = true)
    public Canal obtenerCanalActivo(String nombreCanal) {
        TipoCanal tipo;
        try {
            tipo = TipoCanal.valueOf(nombreCanal.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("CHANNEL_INACTIVE", "El canal " + nombreCanal + " no existe");
        }

        Canal canal = canalRepository.findByTipo(tipo).orElse(null);
        if (canal == null || !canal.estaHabilitado()) {
            throw new ReglaNegocioException("CHANNEL_INACTIVE",
                    "El canal " + tipo + " no existe o está inactivo");
        }
        return canal;
    }
}
