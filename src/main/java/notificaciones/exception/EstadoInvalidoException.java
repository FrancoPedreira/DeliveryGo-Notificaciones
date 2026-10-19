package notificaciones.exception;

// 409: la operación no se puede aplicar en el estado actual de la notificación
// (NOTIFICATION_NOT_RETRYABLE, MAX_RETRIES_EXCEEDED, NOTIFICATION_NOT_SENDABLE)
public class EstadoInvalidoException extends NotificacionesException {
    public EstadoInvalidoException(String code, String message) {
        super(code, message);
    }
}
