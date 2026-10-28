package notificaciones.exception;

// 409: el recurso que se intenta crear ya existe
public class RecursoDuplicadoException extends NotificacionesException {
    public RecursoDuplicadoException(String code, String message) {
        super(code, message);
    }
}
