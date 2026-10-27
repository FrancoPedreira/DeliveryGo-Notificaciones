package notificaciones.exception;

// 400: datos obligatorios faltantes o con formato inválido (RN-01)
public class ValidacionException extends NotificacionesException {
    public ValidacionException(String message) {
        super("VALIDATION_ERROR", message);
    }
}
