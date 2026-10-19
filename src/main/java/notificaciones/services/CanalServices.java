package notificaciones.services;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import notificaciones.dto.CanalDto;
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
            throw new IllegalArgumentException("El tipo de canal " + canalDto.getTipo() + " no existe");
        }
        if (canalRepository.findByTipo(tipo) != null) {
            throw new IllegalArgumentException("El canal " + tipo + " ya existe");
        }

        Canal canal = new Canal(tipo, canalDto.getNombre(), canalDto.isActivo());
        canalRepository.save(canal);
        return canalDto;
    }
}
