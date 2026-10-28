package notificaciones.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
}
