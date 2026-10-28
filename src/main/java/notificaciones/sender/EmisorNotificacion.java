package notificaciones.sender;

import notificaciones.model.Notificacion;
import notificaciones.model.TipoCanal;

// DM-07: un emisor por canal; permite conectar un proveedor real sin cambiar el dominio
public interface EmisorNotificacion {
    TipoCanal getTipoCanal();

    ResultadoEnvio enviar(Notificacion notificacion);
}
