package notificaciones.sender;

import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import notificaciones.model.Notificacion;
import notificaciones.model.TipoCanal;

// Simula un proveedor SMTP: no requiere credenciales y falla según la tasa configurada
@Component
public class EmisorEmailSimulado implements EmisorNotificacion {
    private static final Logger log = LoggerFactory.getLogger(EmisorEmailSimulado.class);
    private final double tasaFallo;

    public EmisorEmailSimulado(@Value("${notificaciones.emisor.tasa-fallo}") double tasaFallo) {
        this.tasaFallo = tasaFallo;
    }

    @Override
    public TipoCanal getTipoCanal() {
        return TipoCanal.EMAIL;
    }

    @Override
    public ResultadoEnvio enviar(Notificacion notificacion) {
        if (ThreadLocalRandom.current().nextDouble() < tasaFallo) {
            return ResultadoEnvio.fallo("Fallo simulado del proveedor EMAIL");
        }
        log.info("EMAIL simulado enviado al destinatario {}", notificacion.getDestinatarioId());
        return ResultadoEnvio.ok();
    }
}
