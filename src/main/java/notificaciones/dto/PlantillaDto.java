package notificaciones.dto;

public class PlantillaDto {
    private String nombre;
    private String asunto;
    private String cuerpo;
    private boolean activa;
    private CanalDto canal;

    public PlantillaDto(String nombre, String asunto, String cuerpo, boolean activa, CanalDto canal) {
        this.nombre = nombre;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
        this.activa = activa;
        this.canal = canal;
    }

    // Getters y Setters
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public void setCuerpo(String cuerpo) {
        this.cuerpo = cuerpo;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public CanalDto getCanal() {
        return canal;
    }

    public void setCanal(CanalDto canal) {
        this.canal = canal;
    }
}
