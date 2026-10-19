package notificaciones.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import notificaciones.dto.IntentoEnvioDto;
import notificaciones.dto.CrearNotificacionRequestDto;
import notificaciones.dto.NotificacionResponseDto;
import notificaciones.exception.EstadoInvalidoException;
import notificaciones.exception.RecursoNoEncontradoException;
import notificaciones.exception.ReglaNegocioException;
import notificaciones.model.Canal;
import notificaciones.model.EstadoNotificacion;
import notificaciones.model.Notificacion;
import notificaciones.model.Plantilla;
import notificaciones.model.ResultadoIntento;
import notificaciones.model.TipoCanal;
import notificaciones.repository.CanalRepository;
import notificaciones.repository.NotificacionRepository;
import notificaciones.repository.PlantillaRepository;
import static notificaciones.utils.Validaciones.*;

@Service
public class NotificacionServices {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{(\\w+)\\}");

    private final NotificacionRepository notificacionRepository;
    private final PlantillaRepository plantillaRepository;
    private final CanalRepository canalRepository;
    private final IntentoEnvioServices intentoEnvioServices;
    private final int maxIntentos;
    private final double tasaFallo;

    public NotificacionServices(NotificacionRepository notificacionRepository,
            PlantillaRepository plantillaRepository,
            CanalRepository canalRepository,
            IntentoEnvioServices intentoEnvioServices,
            @Value("${notificaciones.max-intentos}") int maxIntentos,
            @Value("${notificaciones.emisor.tasa-fallo}") double tasaFallo) {
        this.notificacionRepository = notificacionRepository;
        this.plantillaRepository = plantillaRepository;
        this.canalRepository = canalRepository;
        this.intentoEnvioServices = intentoEnvioServices;
        this.maxIntentos = maxIntentos;
        this.tasaFallo = tasaFallo;
    }

    // RN-01
    private CrearNotificacionRequestDto validarNotificacion(CrearNotificacionRequestDto notificacionDto) {
        validateId(notificacionDto.getDestinatarioId(), "destinatarioId");
        validateId(notificacionDto.getPlantillaId(), "plantillaId");
        validateRequiredText(notificacionDto.getCanal(), "canal");
        validateParametros(notificacionDto.getParametros());
        return notificacionDto;
    }

    // RN-07
    private Canal obtenerCanalActivo(String nombreCanal) {
        TipoCanal tipo;
        try {
            tipo = TipoCanal.valueOf(nombreCanal.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("CHANNEL_INACTIVE", "El canal " + nombreCanal + " no existe");
        }

        Canal canal = canalRepository.findByTipo(tipo);
        if (canal == null || !canal.isActivo()) {
            throw new ReglaNegocioException("CHANNEL_INACTIVE",
                    "El canal " + tipo + " no existe o está inactivo");
        }
        return canal;
    }

    private Notificacion obtenerNotificacion(Long id) {
        return notificacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("NOTIFICATION_NOT_FOUND",
                        "La notificación con id " + id + " no existe"));
    }

    // Entidad → DTO-02: la entidad no sale del service
    private NotificacionResponseDto toDto(Notificacion notificacion) {
        return new NotificacionResponseDto(notificacion.getId(), notificacion.getDestinatarioId(),
                notificacion.getPlantilla().getId(), notificacion.getCanal().getTipo().name(),
                notificacion.getEstado().name(), notificacion.getCantidadIntentos(),
                notificacion.getFechaCreacion(), notificacion.getFechaUltimoIntento());
    }

    // SRV-01 (CU-01)
    @Transactional
    public NotificacionResponseDto crearNotificacion(CrearNotificacionRequestDto notificacionDto) {
        validarNotificacion(notificacionDto);

        Canal canal = obtenerCanalActivo(notificacionDto.getCanal());

        // INT-01 pendiente: la validación del destinatario contra Usuarios se agrega al integrar el módulo

        Notificacion notificacion = new Notificacion(notificacionDto.getDestinatarioId(), LocalDateTime.now(),
                EstadoNotificacion.PENDIENTE);
        notificacion.setCanal(canal);

        // Si la plantilla es inválida se lanza la excepción y la transacción se revierte
        generarMensaje(notificacion, notificacionDto.getPlantillaId(), notificacionDto.getParametros());
        notificacionRepository.save(notificacion);

        procesarEnvio(notificacion);

        return toDto(notificacion);
    }

    // SRV-02 (CU-02)
    private void generarMensaje(Notificacion notificacion, Long plantillaId, Map<String, String> parametros) {
        // RN-02
        Plantilla plantilla = plantillaRepository.findById(plantillaId).orElse(null);
        if (plantilla == null || !plantilla.isActiva()
                || !plantilla.getCanal().getId().equals(notificacion.getCanal().getId())) {
            throw new ReglaNegocioException("TEMPLATE_INVALID", "La plantilla " + plantillaId
                    + " no existe, está inactiva o pertenece a otro canal");
        }

        // RN-08 y RN-06: solo se toman los parámetros que usa la plantilla, el resto se descarta
        List<String> faltantes = new ArrayList<>();
        StringBuilder mensaje = new StringBuilder();
        Matcher matcher = VARIABLE_PATTERN.matcher(plantilla.getCuerpo());
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

        notificacion.setPlantilla(plantilla);
        notificacion.setMensaje(mensaje.toString());
    }

    // SRV-03 (CU-03)
    private void procesarEnvio(Notificacion notificacion) {
        // RN-03
        if (notificacion.getEstado() != EstadoNotificacion.PENDIENTE || notificacion.getMensaje() == null) {
            throw new EstadoInvalidoException("NOTIFICATION_NOT_SENDABLE",
                    "La notificación no está pendiente o no tiene mensaje generado");
        }

        ResultadoIntento resultado = ResultadoIntento.EXITOSO;
        String detalleError = null;

        // RN-07
        if (!notificacion.getCanal().isActivo()) {
            resultado = ResultadoIntento.FALLIDO;
            detalleError = "El canal " + notificacion.getCanal().getTipo() + " está inactivo";
        } else {
            detalleError = enviarSimulado(notificacion.getCanal().getTipo());
            if (detalleError != null) {
                resultado = ResultadoIntento.FALLIDO;
            }
        }

        registrarIntento(notificacion, resultado, detalleError);
    }

    // DM-07: EMAIL y PUSH se simulan con una tasa de fallo; IN_APP queda entregada al persistirse
    private String enviarSimulado(TipoCanal tipo) {
        if (tipo != TipoCanal.IN_APP && ThreadLocalRandom.current().nextDouble() < tasaFallo) {
            return "Fallo simulado del proveedor " + tipo;
        }
        return null;
    }

    // RN-04 y DM-08: el intento y el contador se actualizan en la misma transacción
    private void registrarIntento(Notificacion notificacion, ResultadoIntento resultado, String detalleError) {
        LocalDateTime ahora = LocalDateTime.now();
        int numeroIntento = notificacion.getCantidadIntentos() + 1;

        intentoEnvioServices.crearIntentoEnvio(
                new IntentoEnvioDto(numeroIntento, ahora, resultado.name(), detalleError), notificacion);

        notificacion.setCantidadIntentos(numeroIntento);
        notificacion.setFechaUltimoIntento(ahora);
        if (resultado == ResultadoIntento.EXITOSO) {
            notificacion.setEstado(EstadoNotificacion.ENVIADA);
        } else if (numeroIntento >= maxIntentos) {
            notificacion.setEstado(EstadoNotificacion.FALLIDA_DEFINITIVA);
        } else {
            notificacion.setEstado(EstadoNotificacion.FALLIDA);
        }
        notificacionRepository.save(notificacion);
    }

    // SRV-04 (CU-04)
    @Transactional
    public NotificacionResponseDto consultarEstado(Long id) {
        return toDto(obtenerNotificacion(id));
    }

    // SRV-05 (CU-05)
    @Transactional
    public NotificacionResponseDto reintentarNotificacion(Long id) {
        Notificacion notificacion = obtenerNotificacion(id);

        // RN-04 y RN-05
        if (notificacion.getEstado() == EstadoNotificacion.FALLIDA_DEFINITIVA) {
            throw new EstadoInvalidoException("MAX_RETRIES_EXCEEDED", "La notificación " + id
                    + " alcanzó el máximo de intentos y ya no puede reintentarse");
        }
        if (notificacion.getEstado() != EstadoNotificacion.FALLIDA) {
            throw new EstadoInvalidoException("NOTIFICATION_NOT_RETRYABLE", "La notificación " + id
                    + " está en estado " + notificacion.getEstado() + " y solo puede reintentarse si está FALLIDA");
        }

        notificacion.setEstado(EstadoNotificacion.PENDIENTE);
        notificacionRepository.save(notificacion);

        procesarEnvio(notificacion);

        return toDto(notificacion);
    }
}
