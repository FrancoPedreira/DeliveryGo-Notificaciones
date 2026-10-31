package notificaciones.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import notificaciones.dto.IntentoEnvioResponseDto;
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

    // REP-05: guarda el intento que la notificación acaba de registrar (DM-08).
    // DM-09: debe ejecutarse en la misma transacción que actualiza el contador y el estado
    @Transactional(propagation = Propagation.MANDATORY)
    public IntentoEnvio registrarIntento(Notificacion notificacion, ResultadoIntento resultado, String detalleError) {
        IntentoEnvio intento = IntentoEnvio.crear(notificacion, notificacion.getCantidadIntentos(), resultado,
                detalleError);
        return intentoEnvioRepository.save(intento);
    }

    // SRV-09 (CU-09, REP-12): los intentos de una notificación en orden; el service de Notificacion valida que exista
    @Transactional(readOnly = true)
    public List<IntentoEnvioResponseDto> listarIntentos(Long notificacionId) {
        return intentoEnvioRepository.findByNotificacionIdOrderByNumeroIntentoAsc(notificacionId).stream()
                .map(intento -> new IntentoEnvioResponseDto(intento.getNumeroIntento(), intento.getFecha(),
                        intento.getResultado().name(), intento.getDetalleError()))
                .toList();
    }
}
