package notificaciones.services;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import notificaciones.client.UsuariosClient;
import jakarta.persistence.criteria.Predicate;
import notificaciones.dto.CrearNotificacionRequestDto;
import notificaciones.dto.IntentoEnvioResponseDto;
import notificaciones.dto.NotificacionResponseDto;
import notificaciones.dto.PaginaResponseDto;
import notificaciones.dto.UsuarioContactoResponseDto;
import notificaciones.exception.EstadoInvalidoException;
import notificaciones.exception.RecursoNoEncontradoException;
import notificaciones.exception.ReglaNegocioException;
import notificaciones.exception.ValidacionException;
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
    private static final int TAMANIO_MAXIMO_PAGINA = 100;
    // RN-11: campos por los que se puede ordenar el historial
    private static final Set<String> CAMPOS_ORDENABLES = Set.of("id", "fechaCreacion", "fechaUltimoIntento", "estado",
            "cantidadIntentos");

    private final NotificacionRepository notificacionRepository;
    private final CanalServices canalServices;
    private final PlantillaServices plantillaServices;
    private final IntentoEnvioServices intentoEnvioServices;
    private final UsuariosClient usuariosClient;
    private final TransactionTemplate transactionTemplate;
    private final Map<TipoCanal, EmisorNotificacion> emisores = new EnumMap<>(TipoCanal.class);
    private final int maxIntentos;

    public NotificacionServices(NotificacionRepository notificacionRepository,
            CanalServices canalServices,
            PlantillaServices plantillaServices,
            IntentoEnvioServices intentoEnvioServices,
            UsuariosClient usuariosClient,
            TransactionTemplate transactionTemplate,
            List<EmisorNotificacion> emisores,
            @Value("${notificaciones.max-intentos}") int maxIntentos) {
        this.notificacionRepository = notificacionRepository;
        this.canalServices = canalServices;
        this.plantillaServices = plantillaServices;
        this.intentoEnvioServices = intentoEnvioServices;
        this.usuariosClient = usuariosClient;
        this.transactionTemplate = transactionTemplate;
        this.maxIntentos = maxIntentos;
        emisores.forEach(emisor -> this.emisores.put(emisor.getTipoCanal(), emisor));
    }

    // RN-01: destinatario y plantilla son obligatorios. El canal es opcional (RN-13)
    private CrearNotificacionRequestDto validarNotificacion(CrearNotificacionRequestDto notificacionDto) {
        validateId(notificacionDto.getDestinatarioId(), "destinatarioId");
        validateId(notificacionDto.getPlantillaId(), "plantillaId");
        return notificacionDto;
    }

    // INT-01 y RN-09: el destinatario debe existir, estar activo y tener el dato de contacto que pide el canal
    private void validarDestinatario(Long destinatarioId, TipoCanal tipoCanal) {
        UsuarioContactoResponseDto contacto = usuariosClient.obtenerContacto(destinatarioId)
                .filter(UsuarioContactoResponseDto::isActivo)
                .orElseThrow(() -> new ReglaNegocioException("RECIPIENT_NOT_FOUND",
                        "El destinatario " + destinatarioId + " no existe o está inactivo"));

        boolean tieneContacto = switch (tipoCanal) {
            case EMAIL -> contacto.getEmail() != null && !contacto.getEmail().isBlank();
            case PUSH -> contacto.getPushToken() != null && !contacto.getPushToken().isBlank();
            case IN_APP -> true;
        };
        if (!tieneContacto) {
            throw new ReglaNegocioException("RECIPIENT_CONTACT_MISSING",
                    "El destinatario " + destinatarioId + " no tiene el dato de contacto que requiere el canal "
                            + tipoCanal);
        }
    }

    private Notificacion obtenerNotificacion(Long id) {
        return notificacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("NOTIFICATION_NOT_FOUND",
                        "La notificación con id " + id + " no existe"));
    }

    // Entidad → DTO-02: la entidad no sale del service. El mensaje solo se informa en el historial (CU-06)
    private NotificacionResponseDto toDto(Notificacion notificacion, boolean incluirMensaje) {
        return new NotificacionResponseDto(notificacion.getId(), notificacion.getDestinatarioId(),
                notificacion.getPlantilla().getId(), notificacion.getCanal().getTipo().name(),
                notificacion.getEstado().name(), notificacion.getCantidadIntentos(),
                notificacion.getFechaCreacion(), notificacion.getFechaUltimoIntento(),
                notificacion.getFechaLectura(), incluirMensaje ? notificacion.getMensaje() : null);
    }

    private NotificacionResponseDto toDto(Notificacion notificacion) {
        return toDto(notificacion, false);
    }

    // SRV-01 (CU-01)
    public NotificacionResponseDto crearNotificacion(CrearNotificacionRequestDto notificacionDto) {
        validarNotificacion(notificacionDto);

        // RN-13 (CU-08): sin canal explícito se usa el de la plantilla. Si se indica uno distinto, RN-02 lo rechaza
        String nombreCanal = notificacionDto.getCanal() != null && !notificacionDto.getCanal().isBlank()
                ? notificacionDto.getCanal()
                : plantillaServices.canalDePlantilla(notificacionDto.getPlantillaId());

        // RN-07
        Canal canal = canalServices.obtenerCanalActivo(nombreCanal);

        // DM-10: INT-01 se consulta antes de abrir la transacción, para no tener una conexión
        // a la base tomada mientras se espera a Usuarios
        validarDestinatario(notificacionDto.getDestinatarioId(), canal.getTipo());

        Map<String, String> parametros = notificacionDto.getParametros() != null ? notificacionDto.getParametros()
                : Map.of();

        // DM-09: alta, mensaje, primer intento, contador y estado se confirman juntos.
        // Se usa TransactionTemplate porque un @Transactional en un método de esta misma clase
        // no pasaría por el proxy de Spring y no abriría la transacción.
        return transactionTemplate.execute(status -> {
            Notificacion notificacion = new Notificacion(notificacionDto.getDestinatarioId(), canal);

            // SRV-02: si la plantilla es inválida se lanza la excepción y la transacción se revierte
            plantillaServices.generarMensaje(notificacion, notificacionDto.getPlantillaId(), parametros);
            notificacionRepository.save(notificacion);

            // Un fallo de envío no lanza excepción: se confirma como FALLIDA con su intento
            procesarEnvio(notificacion);

            return toDto(notificacion);
        });
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
    @Transactional(readOnly = true)
    public NotificacionResponseDto consultarEstado(Long id) {
        return toDto(obtenerNotificacion(id));
    }

    // SRV-05 (CU-05). DM-09: una sola transacción con la notificación bloqueada hasta el commit
    @Transactional
    public NotificacionResponseDto reintentarNotificacion(Long id) {
        // REP-07: un segundo reintento simultáneo espera acá y después ve el estado que dejó el primero
        Notificacion notificacion = notificacionRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("NOTIFICATION_NOT_FOUND",
                        "La notificación con id " + id + " no existe"));

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

    // SRV-06 (CU-06). RN-11: historial del destinatario, paginado y filtrable. No consulta a Usuarios (INT-01):
    // el historial de un usuario sigue disponible aunque Usuarios no responda o el usuario ya esté inactivo
    @Transactional(readOnly = true)
    public PaginaResponseDto<NotificacionResponseDto> consultarHistorial(Long usuarioId, String estado, String canal,
            Boolean leida, int page, int size, String sort) {
        validateId(usuarioId, "id");
        EstadoNotificacion estadoFiltro = parsearEnum(EstadoNotificacion.class, estado, "estado");
        TipoCanal canalFiltro = parsearEnum(TipoCanal.class, canal, "canal");
        if (page < 0) {
            throw new ValidacionException("El campo page no puede ser negativo");
        }
        if (size < 1 || size > TAMANIO_MAXIMO_PAGINA) {
            throw new ValidacionException("El campo size debe estar entre 1 y " + TAMANIO_MAXIMO_PAGINA);
        }

        // REP-08
        Specification<Notificacion> filtro = (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.equal(root.get("destinatarioId"), usuarioId));
            if (estadoFiltro != null) {
                condiciones.add(cb.equal(root.get("estado"), estadoFiltro));
            }
            if (canalFiltro != null) {
                condiciones.add(cb.equal(root.get("canal").get("tipo"), canalFiltro));
            }
            if (leida != null) {
                condiciones.add(leida ? cb.isNotNull(root.get("fechaLectura")) : cb.isNull(root.get("fechaLectura")));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };

        Page<Notificacion> pagina = notificacionRepository.findAll(filtro,
                PageRequest.of(page, size, parsearOrden(sort)));
        return new PaginaResponseDto<>(pagina.getContent().stream().map(n -> toDto(n, true)).toList(),
                pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
    }

    // "campo" o "campo,asc|desc"; por defecto las más recientes primero. Con otro campo la base fallaría con un 500
    private Sort parsearOrden(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "fechaCreacion", "id");
        }
        String[] partes = sort.split(",");
        String campo = partes[0].trim();
        if (!CAMPOS_ORDENABLES.contains(campo)) {
            throw new ValidacionException("No se puede ordenar por " + campo + ". Campos válidos: "
                    + String.join(", ", CAMPOS_ORDENABLES.stream().sorted().toList()));
        }
        Sort.Direction direccion = Sort.Direction.ASC;
        if (partes.length > 1) {
            direccion = switch (partes[1].trim().toLowerCase()) {
                case "asc" -> Sort.Direction.ASC;
                case "desc" -> Sort.Direction.DESC;
                default -> throw new ValidacionException("La dirección del orden debe ser asc o desc");
            };
        }
        return Sort.by(direccion, campo).and(Sort.by(direccion, "id"));
    }

    private <E extends Enum<E>> E parsearEnum(Class<E> tipo, String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidacionException("El valor " + valor + " no es válido para el campo " + campo);
        }
    }

    // SRV-09 (CU-09)
    @Transactional(readOnly = true)
    public List<IntentoEnvioResponseDto> consultarIntentos(Long id) {
        // Falla con 404 si la notificación no existe, para no confundirlo con una notificación sin intentos
        obtenerNotificacion(id);
        return intentoEnvioServices.listarIntentos(id);
    }

    // SRV-10 (CU-10). RN-12: idempotente, la segunda lectura devuelve la misma fecha de lectura
    @Transactional
    public NotificacionResponseDto marcarComoLeida(Long id) {
        Notificacion notificacion = obtenerNotificacion(id);

        if (notificacion.getCanal().getTipo() != TipoCanal.IN_APP) {
            throw new ReglaNegocioException("NOTIFICATION_NOT_IN_APP", "La notificación " + id
                    + " es del canal " + notificacion.getCanal().getTipo()
                    + " y solo las IN_APP pueden marcarse como leídas");
        }
        if (!notificacion.puedeMarcarseComoLeida()) {
            throw new EstadoInvalidoException("NOTIFICATION_NOT_DELIVERED", "La notificación " + id
                    + " está en estado " + notificacion.getEstado() + " y solo puede leerse una vez ENVIADA");
        }

        notificacion.marcarComoLeida();
        return toDto(notificacionRepository.save(notificacion));
    }
}
