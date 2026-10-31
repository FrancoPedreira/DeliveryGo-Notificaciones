package notificaciones.utils;

import notificaciones.exception.ValidacionException;

public final class Validaciones {

    private Validaciones() {
    }

    public static Long validateId(Long id, String campo) {
        if (id == null) {
            throw new ValidacionException("El campo " + campo + " no puede ser nulo");
        } else if (id <= 0) {
            throw new ValidacionException("El campo " + campo + " debe ser mayor a cero");
        }
        return id;
    }

    public static String validateRequiredText(String input, String campo) {
        if (input == null || input.isBlank()) {
            throw new ValidacionException("El campo " + campo + " no puede ser nulo o vacío");
        }
        return input;
    }

    public static String validateMaxLength(String input, String campo, int maximo) {
        if (input != null && input.length() > maximo) {
            throw new ValidacionException("El campo " + campo + " no puede superar los " + maximo + " caracteres");
        }
        return input;
    }

    public static String validateValidCharacters(String input, String campo) {
        validateRequiredText(input, campo);
        if (!input.matches("^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ_\s]+$")) {
            throw new ValidacionException("El campo " + campo + " contiene caracteres inválidos");
        }
        return input;
    }
}
