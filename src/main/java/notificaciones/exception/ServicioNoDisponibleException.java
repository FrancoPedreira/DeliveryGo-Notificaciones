package notificaciones.exception;

// 503: un módulo del que dependemos no respondió (USERS_SERVICE_UNAVAILABLE)
public class ServicioNoDisponibleException extends NotificacionesException {
    public ServicioNoDisponibleException(String code, String message) {
        super(code, message);
    }
}
