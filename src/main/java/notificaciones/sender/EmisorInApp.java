package notificaciones.sender;

import org.springframework.stereotype.Component;

import notificaciones.model.Notificacion;
import notificaciones.model.TipoCanal;

// IN_APP no usa proveedor externo: la notificación queda entregada al persistirse en la bandeja interna
@Component
public class EmisorInApp implements EmisorNotificacion {

    @Override
    public TipoCanal getTipoCanal() {
        return TipoCanal.IN_APP;
    }

    @Override
    public ResultadoEnvio enviar(Notificacion notificacion) {
        return ResultadoEnvio.ok();
    }
}
