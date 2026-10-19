package notificaciones.utils;

import java.util.Map;

public final class Validaciones {

    public static Long validateId(Long id, String campo) {
        if (id == null) {
            throw new IllegalArgumentException("El campo " + campo + " no puede ser nulo");
        } else if (id <= 0) {
            throw new IllegalArgumentException("El campo " + campo + " debe ser mayor a cero");
        }
        return id;
    }

    public static String validateRequiredText(String input, String campo) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " no puede ser nulo o vacío");
        }
        return input;
    }

    public static String validateValidCharacters(String input, String campo) {
        validateRequiredText(input, campo);
        if (!input.matches("^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ_\\s]+$")) {
            throw new IllegalArgumentException("El campo " + campo + " contiene caracteres inválidos");
        }
        return input;
    }

    public static Map<String, String> validateParametros(Map<String, String> parametros) {
        if (parametros == null) {
            throw new IllegalArgumentException("El campo parametros no puede ser nulo");
        }
        return parametros;
    }
}
