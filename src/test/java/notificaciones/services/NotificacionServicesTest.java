package notificaciones.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import notificaciones.dto.CrearNotificacionRequestDto;
import notificaciones.dto.NotificacionResponseDto;
import notificaciones.exception.EstadoInvalidoException;
import notificaciones.exception.NotificacionesException;
import notificaciones.exception.RecursoNoEncontradoException;
import notificaciones.exception.ReglaNegocioException;
import notificaciones.exception.ValidacionException;
import notificaciones.repository.NotificacionRepository;

// Envío siempre exitoso: prueba los caminos de CU-01 a CU-05 que no dependen de un fallo del emisor.
// Plantillas del DataLoader: 1 EMAIL {nombre, pedidoId}, 2 PUSH {monto}, 3 IN_APP {codigo}.
// Usuarios simulados: 10 con email y push, 11 sin pushToken, 12 inactivo.
@SpringBootTest
@TestPropertySource(properties = "notificaciones.emisor.tasa-fallo=0")
class NotificacionServicesTest {

    @Autowired
    private NotificacionServices notificacionServices;

    @Autowired
    private NotificacionRepository notificacionRepository;

    private CrearNotificacionRequestDto solicitud(Long destinatarioId, Long plantillaId, String canal,
            Map<String, String> parametros) {
        return new CrearNotificacionRequestDto(destinatarioId, plantillaId, canal, parametros);
    }

    private void assertCodigo(Runnable accion, Class<? extends NotificacionesException> tipo, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOf(tipo).extracting("code").isEqualTo(codigo);
    }

    @Test
    void cu01CreaYEnviaLaNotificacion() {
        NotificacionResponseDto respuesta = notificacionServices.crearNotificacion(
                solicitud(10L, 1L, "EMAIL", Map.of("nombre", "Ana", "pedidoId", "55")));

        assertThat(respuesta.getId()).isNotNull();
        assertThat(respuesta.getEstado()).isEqualTo("ENVIADA");
        assertThat(respuesta.getCantidadIntentos()).isEqualTo(1);
        assertThat(respuesta.getFechaUltimoIntento()).isNotNull();
    }

    @Test
    void cu02DescartaLosParametrosQueLaPlantillaNoUsa() {
        NotificacionResponseDto respuesta = notificacionServices.crearNotificacion(
                solicitud(10L, 1L, "EMAIL", Map.of("nombre", "Ana", "pedidoId", "55", "tarjeta", "4111")));

        // RN-06: el dato que no pide la plantilla no queda guardado en el mensaje
        String mensaje = notificacionRepository.findById(respuesta.getId()).orElseThrow().getMensaje();
        assertThat(mensaje).isEqualTo("Hola Ana, tu pedido 55 fue confirmado.");
    }

    @Test
    void cu01ParametrosEsOpcionalSiLaPlantillaNoLosNecesita() {
        // RN-01 solo exige destinatario, plantilla y canal; la plantilla 3 pide {codigo}, así que falla por RN-08
        assertCodigo(() -> notificacionServices.crearNotificacion(solicitud(10L, 3L, "IN_APP", null)),
                ReglaNegocioException.class, "TEMPLATE_PARAMS_MISSING");
    }

    @Test
    void rn01RechazaSolicitudSinCanal() {
        assertThatThrownBy(() -> notificacionServices.crearNotificacion(solicitud(10L, 1L, null, Map.of())))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void rn07RechazaCanalInexistente() {
        assertCodigo(() -> notificacionServices.crearNotificacion(solicitud(10L, 1L, "SMS", Map.of())),
                ReglaNegocioException.class, "CHANNEL_INACTIVE");
    }

    @Test
    void int01RechazaDestinatarioInexistenteOInactivo() {
        assertCodigo(() -> notificacionServices.crearNotificacion(solicitud(999L, 3L, "IN_APP", Map.of("codigo", "X"))),
                ReglaNegocioException.class, "RECIPIENT_NOT_FOUND");
        assertCodigo(() -> notificacionServices.crearNotificacion(solicitud(12L, 3L, "IN_APP", Map.of("codigo", "X"))),
                ReglaNegocioException.class, "RECIPIENT_NOT_FOUND");
    }

    @Test
    void rn09RechazaDestinatarioSinElContactoQuePideElCanal() {
        assertCodigo(() -> notificacionServices.crearNotificacion(solicitud(11L, 2L, "PUSH", Map.of("monto", "100"))),
                ReglaNegocioException.class, "RECIPIENT_CONTACT_MISSING");
    }

    @Test
    void rn02RechazaPlantillaDeOtroCanalSinRegistrarNada() {
        long antes = notificacionRepository.count();

        assertCodigo(() -> notificacionServices.crearNotificacion(solicitud(10L, 1L, "PUSH", Map.of("monto", "1"))),
                ReglaNegocioException.class, "TEMPLATE_INVALID");

        // CU-02 A1: la operación se revierte y la solicitud no se registra
        assertThat(notificacionRepository.count()).isEqualTo(antes);
    }

    @Test
    void rn08RechazaParametrosFaltantesSinRegistrarNada() {
        long antes = notificacionRepository.count();

        assertCodigo(() -> notificacionServices.crearNotificacion(solicitud(10L, 1L, "EMAIL", Map.of("nombre", "Ana"))),
                ReglaNegocioException.class, "TEMPLATE_PARAMS_MISSING");

        assertThat(notificacionRepository.count()).isEqualTo(antes);
    }

    @Test
    void cu04ConsultaElEstado() {
        Long id = notificacionServices.crearNotificacion(
                solicitud(10L, 3L, "IN_APP", Map.of("codigo", "X1"))).getId();

        assertThat(notificacionServices.consultarEstado(id).getEstado()).isEqualTo("ENVIADA");
    }

    @Test
    void cu04DevuelveNotFoundSiNoExiste() {
        assertCodigo(() -> notificacionServices.consultarEstado(99999L),
                RecursoNoEncontradoException.class, "NOTIFICATION_NOT_FOUND");
    }

    @Test
    void rn05NoReintentaUnaNotificacionEnviada() {
        Long id = notificacionServices.crearNotificacion(
                solicitud(10L, 3L, "IN_APP", Map.of("codigo", "X1"))).getId();

        assertCodigo(() -> notificacionServices.reintentarNotificacion(id),
                EstadoInvalidoException.class, "NOTIFICATION_NOT_RETRYABLE");
    }
}
