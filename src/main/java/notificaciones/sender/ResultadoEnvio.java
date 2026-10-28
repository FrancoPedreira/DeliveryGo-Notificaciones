package notificaciones.sender;

// Lo que devuelve un emisor: si salió bien o el motivo técnico del fallo (sin contenido del mensaje, RN-06)
public record ResultadoEnvio(boolean exitoso, String detalleError) {

    public static ResultadoEnvio ok() {
        return new ResultadoEnvio(true, null);
    }

    public static ResultadoEnvio fallo(String detalleError) {
        return new ResultadoEnvio(false, detalleError);
    }
}
