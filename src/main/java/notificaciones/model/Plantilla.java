package notificaciones.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.persistence.*;
import notificaciones.exception.ReglaNegocioException;

@Entity
@Table(name = "plantilla")
public class Plantilla {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{(\\w+)\\}");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private String asunto;

    // Cuerpo con variables entre llaves, ej: "Hola {nombre}, tu pedido {pedidoId} fue confirmado"
    @Column(nullable = false, length = 1000)
    private String cuerpo;

    @Column(nullable = false)
    private boolean activa;

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "canal_id", nullable = false)
    private Canal canal;

    protected Plantilla() {
    }

    public Plantilla(String nombre, String asunto, String cuerpo, boolean activa, Canal canal) {
        this.nombre = nombre;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
        this.activa = activa;
        this.canal = canal;
    }

    // RN-08 y RN-06: solo se toman los parámetros que usa la plantilla, el resto se descarta
    public String renderizar(Map<String, String> parametros) {
        List<String> faltantes = new ArrayList<>();
        StringBuilder mensaje = new StringBuilder();
        Matcher matcher = VARIABLE_PATTERN.matcher(cuerpo);
        while (matcher.find()) {
            String valor = parametros.get(matcher.group(1));
            if (valor == null || valor.isBlank()) {
                faltantes.add(matcher.group(1));
            } else {
                matcher.appendReplacement(mensaje, Matcher.quoteReplacement(valor));
            }
        }
        matcher.appendTail(mensaje);

        if (!faltantes.isEmpty()) {
            throw new ReglaNegocioException("TEMPLATE_PARAMS_MISSING",
                    "Faltan los parámetros " + String.join(", ", faltantes));
        }
        return mensaje.toString();
    }

    // CU-07: variables que exige el cuerpo, en orden de aparición y sin repetir
    public List<String> variables() {
        List<String> variables = new ArrayList<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(cuerpo);
        while (matcher.find()) {
            if (!variables.contains(matcher.group(1))) {
                variables.add(matcher.group(1));
            }
        }
        return variables;
    }

    // CU-07 y RN-10: el canal no se modifica; los mensajes ya generados tampoco, porque se guardan en la Notificacion
    public void modificar(String nombre, String asunto, String cuerpo, boolean activa) {
        this.nombre = nombre;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
        this.activa = activa;
    }

    // RN-02
    public boolean admiteCanal(Canal canal) {
        return this.canal.getId().equals(canal.getId());
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getAsunto() {
        return asunto;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public boolean isActiva() {
        return activa;
    }

    public Canal getCanal() {
        return canal;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
