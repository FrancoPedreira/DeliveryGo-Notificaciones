package notificaciones.exception;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import notificaciones.dto.ErrorResponseDto;

// Traduce las excepciones a DTO-03 para todas las APIs del módulo (API-01 a API-03)
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponseDto> handleNoEncontrado(RecursoNoEncontradoException e,
            HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, e.getCode(), e.getMessage(), request);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponseDto> handleReglaNegocio(ReglaNegocioException e,
            HttpServletRequest request) {
        return construir(HttpStatus.UNPROCESSABLE_CONTENT, e.getCode(), e.getMessage(), request);
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    public ResponseEntity<ErrorResponseDto> handleEstadoInvalido(EstadoInvalidoException e,
            HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, e.getCode(), e.getMessage(), request);
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicado(RecursoDuplicadoException e,
            HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, e.getCode(), e.getMessage(), request);
    }

    // RN-01: datos obligatorios faltantes o inválidos (los lanza Validaciones)
    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<ErrorResponseDto> handleValidacion(ValidacionException e,
            HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, e.getCode(), e.getMessage(), request);
    }

    // Body con JSON mal formado o id no numérico en la URL
    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    public ResponseEntity<ErrorResponseDto> handleRequestMalFormado(Exception e, HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "La solicitud tiene un formato inválido",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenerico(Exception e, HttpServletRequest request) {
        // Errores propios de Spring MVC (ruta inexistente, método no permitido, etc.) conservan su status
        if (e instanceof org.springframework.web.ErrorResponse errorSpring) {
            HttpStatus status = HttpStatus.resolve(errorSpring.getStatusCode().value());
            if (status != null) {
                return construir(status, status.name(), status.getReasonPhrase(), request);
            }
        }

        // No se expone el detalle interno al cliente; queda en el log con el correlationId
        ResponseEntity<ErrorResponseDto> respuesta = construir(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocurrió un error inesperado", request);
        log.error("Error no controlado [correlationId={}]", respuesta.getBody().getCorrelationId(), e);
        return respuesta;
    }

    private ResponseEntity<ErrorResponseDto> construir(HttpStatus status, String code, String message,
            HttpServletRequest request) {
        // Se reutiliza el correlationId del llamador si lo envía, para seguir la solicitud entre módulos
        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        ErrorResponseDto body = new ErrorResponseDto(LocalDateTime.now(), status.value(), code, message,
                request.getRequestURI(), correlationId);
        return ResponseEntity.status(status).header(CORRELATION_HEADER, correlationId).body(body);
    }
}
