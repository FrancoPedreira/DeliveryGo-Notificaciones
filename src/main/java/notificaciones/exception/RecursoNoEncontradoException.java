package notificaciones.exception;

// 404: el recurso solicitado no existe (NOTIFICATION_NOT_FOUND)
public class RecursoNoEncontradoException extends NotificacionesException {
    public RecursoNoEncontradoException(String code, String message) {
        super(code, message);
    }
}
