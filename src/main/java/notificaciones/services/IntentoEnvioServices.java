package notificaciones.services;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import notificaciones.dto.IntentoEnvioDto;
import notificaciones.model.IntentoEnvio;
import notificaciones.model.Notificacion;
import notificaciones.model.ResultadoIntento;
import notificaciones.repository.IntentoEnvioRepository;

@Service
public class IntentoEnvioServices {
    private final IntentoEnvioRepository intentoEnvioRepository;

    public IntentoEnvioServices(IntentoEnvioRepository intentoEnvioRepository) {
        this.intentoEnvioRepository = intentoEnvioRepository;
    }

    @Transactional
    public IntentoEnvioDto crearIntentoEnvio(IntentoEnvioDto intentoEnvioDto, Notificacion notificacion) {
        IntentoEnvio intentoEnvio = new IntentoEnvio(intentoEnvioDto.getNumeroIntento(),
                intentoEnvioDto.getFecha(), ResultadoIntento.valueOf(intentoEnvioDto.getResultado()),
                intentoEnvioDto.getDetalleError());
        intentoEnvio.setNotificacion(notificacion);
        intentoEnvioRepository.save(intentoEnvio);
        return intentoEnvioDto;
    }
}
