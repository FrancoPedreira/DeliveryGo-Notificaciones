package notificaciones.exception;

// 400: datos obligatorios faltantes o con formato inválido (RN-01, RN-10, RN-11 y el campo activo de CU-08)
public class ValidacionException extends NotificacionesException {
    public ValidacionException(String message) {
        super("VALIDATION_ERROR", message);
    }
}
