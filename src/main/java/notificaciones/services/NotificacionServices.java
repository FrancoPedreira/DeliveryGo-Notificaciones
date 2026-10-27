package notificaciones.services;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import notificaciones.dto.CrearNotificacionRequestDto;
import notificaciones.dto.NotificacionResponseDto;
import notificaciones.exception.EstadoInvalidoException;
import notificaciones.exception.RecursoNoEncontradoException;
import notificaciones.model.Canal;
import notificaciones.model.EstadoNotificacion;
import notificaciones.model.Notificacion;
import notificaciones.model.ResultadoIntento;
import notificaciones.model.TipoCanal;
import notificaciones.repository.NotificacionRepository;
import notificaciones.sender.EmisorNotificacion;
import notificaciones.sender.ResultadoEnvio;
import static notificaciones.utils.Validaciones.*;

// Orquesta los casos de uso CORE; delega en el service de cada entidad
@Service
public class NotificacionServices {
    private final NotificacionRepository notificacionRepository;
    private final CanalServices canalServices;
    private final PlantillaServices plantillaServices;
    private final IntentoEnvioServices intentoEnvioServices;
    private final Map<TipoCanal, EmisorNotificacion> emisores = new EnumMap<>(TipoCanal.class);
    private final int maxIntentos;

    public NotificacionServices(NotificacionRepository notificacionRepository,
            CanalServices canalServices,
            PlantillaServices plantillaServices,
            IntentoEnvioServices intentoEnvioServices,
            List<EmisorNotificacion> emisores,
            @Value("${notificaciones.max-intentos}") int maxIntentos) {
        this.notificacionRepository = notificacionRepository;
        this.canalServices = canalServices;
        this.plantillaServices = plantillaServices;
        this.intentoEnvioServices = intentoEnvioServices;
        this.maxIntentos = maxIntentos;
        emisores.forEach(emisor -> this.emisores.put(emisor.getTipoCanal(), emisor));
    }

    // RN-01
    private CrearNotificacionRequestDto validarNotificacion(CrearNotificacionRequestDto notificacionDto) {
        validateId(notificacionDto.getDestinatarioId(), "destinatarioId");
        validateId(notificacionDto.getPlantillaId(), "plantillaId");
        validateRequiredText(notificacionDto.getCanal(), "canal");
        return notificacionDto;
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

        // RN-07
        Canal canal = canalServices.obtenerCanalActivo(notificacionDto.getCanal());

        // INT-01 pendiente: la validación del destinatario contra Usuarios se agrega al integrar el módulo

        Notificacion notificacion = new Notificacion(notificacionDto.getDestinatarioId(), canal);

        // SRV-02: si la plantilla es inválida se lanza la excepción y la transacción se revierte
        Map<String, String> parametros = notificacionDto.getParametros() != null ? notificacionDto.getParametros()
                : Map.of();
        plantillaServices.generarMensaje(notificacion, notificacionDto.getPlantillaId(), parametros);
        notificacionRepository.save(notificacion);

        procesarEnvio(notificacion);

        return toDto(notificacion);
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
        if (!notificacion.getCanal().estaHabilitado()) {
            resultado = ResultadoIntento.FALLIDO;
            detalleError = "El canal " + notificacion.getCanal().getTipo() + " está inactivo";
        } else {
            // DM-07: envía el emisor del canal (EMAIL/PUSH simulados, IN_APP bandeja interna)
            ResultadoEnvio envio = emisores.get(notificacion.getCanal().getTipo()).enviar(notificacion);
            if (!envio.exitoso()) {
                resultado = ResultadoIntento.FALLIDO;
                detalleError = envio.detalleError();
            }
        }

        // RN-04 y DM-08: el contador, el estado y el IntentoEnvio se actualizan en la misma transacción
        notificacion.registrarIntento(resultado, maxIntentos);
        intentoEnvioServices.registrarIntento(notificacion, resultado, detalleError);
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
        if (!notificacion.puedeReintentarse()) {
            throw new EstadoInvalidoException("NOTIFICATION_NOT_RETRYABLE", "La notificación " + id
                    + " está en estado " + notificacion.getEstado() + " y solo puede reintentarse si está FALLIDA");
        }

        notificacion.volverAPendiente();
        notificacionRepository.save(notificacion);

        procesarEnvio(notificacion);

        return toDto(notificacion);
    }
}
