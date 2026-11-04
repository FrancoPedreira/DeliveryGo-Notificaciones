package notificaciones.exception;

// 422: la solicitud viola una regla de negocio (CHANNEL_INACTIVE, TEMPLATE_INVALID, TEMPLATE_PARAMS_MISSING,
// RECIPIENT_NOT_FOUND, RECIPIENT_CONTACT_MISSING, TEMPLATE_CHANNEL_IMMUTABLE, NOTIFICATION_NOT_IN_APP)
public class ReglaNegocioException extends NotificacionesException {
    public ReglaNegocioException(String code, String message) {
        super(code, message);
    }
}
