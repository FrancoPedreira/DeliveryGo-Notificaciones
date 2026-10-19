package notificaciones.exception;

// Base de las excepciones de negocio: cada una lleva el código de error que viaja en DTO-03.
// Extiende de RuntimeException para que @Transactional haga rollback automáticamente.
public abstract class NotificacionesException extends RuntimeException {
    private final String code;

    protected NotificacionesException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
