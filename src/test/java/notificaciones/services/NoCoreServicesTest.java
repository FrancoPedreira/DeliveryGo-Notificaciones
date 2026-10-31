package notificaciones.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import notificaciones.dto.CrearNotificacionRequestDto;
import notificaciones.dto.IntentoEnvioResponseDto;
import notificaciones.dto.NotificacionResponseDto;
import notificaciones.dto.PaginaResponseDto;
import notificaciones.dto.PlantillaRequestDto;
import notificaciones.dto.PlantillaResponseDto;
import notificaciones.exception.EstadoInvalidoException;
import notificaciones.exception.NotificacionesException;
import notificaciones.exception.RecursoDuplicadoException;
import notificaciones.exception.RecursoNoEncontradoException;
import notificaciones.exception.ReglaNegocioException;
import notificaciones.exception.ValidacionException;
import notificaciones.model.EstadoNotificacion;
import notificaciones.model.Notificacion;
import notificaciones.repository.NotificacionRepository;

// CU-06, CU-07, CU-09 y CU-10 (y la selección de canal de CU-08) con envío siempre exitoso.
// Los tests comparten la base: comparan por los ids que crean, no por totales.
// Plantillas del DataLoader: 1 EMAIL {nombre, pedidoId}, 2 PUSH {monto}, 3 IN_APP {codigo}.
// Usuarios simulados: 10 con email y push, 11 sin pushToken, 12 inactivo.
@SpringBootTest
@TestPropertySource(properties = "notificaciones.emisor.tasa-fallo=0")
class NoCoreServicesTest {

    @Autowired
    private NotificacionServices notificacionServices;

    @Autowired
    private PlantillaServices plantillaServices;

    @Autowired
    private NotificacionRepository notificacionRepository;

    private Long crearEmail(Long destinatarioId) {
        return notificacionServices.crearNotificacion(new CrearNotificacionRequestDto(destinatarioId, 1L, "EMAIL",
                Map.of("nombre", "Ana", "pedidoId", "55"))).getId();
    }

    private Long crearInApp(Long destinatarioId) {
        return notificacionServices.crearNotificacion(new CrearNotificacionRequestDto(destinatarioId, 3L, "IN_APP",
                Map.of("codigo", "ABC"))).getId();
    }

    private void assertCodigo(Runnable accion, Class<? extends NotificacionesException> tipo, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOf(tipo).extracting("code").isEqualTo(codigo);
    }

    private PaginaResponseDto<NotificacionResponseDto> historial(Long usuarioId, String estado, String canal,
            Boolean leida, int page, int size, String sort) {
        return notificacionServices.consultarHistorial(usuarioId, estado, canal, leida, page, size, sort);
    }

    // ---------- CU-06 ----------

    @Test
    void cu06DevuelveSoloLasNotificacionesDelUsuarioLasMasRecientesPrimero() {
        Long primera = crearEmail(11L);
        Long segunda = crearInApp(11L);
        Long deOtroUsuario = crearEmail(10L);

        List<NotificacionResponseDto> items = historial(11L, null, null, null, 0, 100, null).getContent();

        assertThat(items).extracting(NotificacionResponseDto::getDestinatarioId).containsOnly(11L);
        assertThat(items).extracting(NotificacionResponseDto::getId)
                .contains(primera, segunda).doesNotContain(deOtroUsuario)
                .isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    @Test
    void cu06IncluyeElMensajeGeneradoSoloEnElHistorial() {
        Long id = crearEmail(11L);

        NotificacionResponseDto item = historial(11L, null, null, null, 0, 100, null).getContent().stream()
                .filter(n -> n.getId().equals(id)).findFirst().orElseThrow();
        assertThat(item.getMensaje()).isEqualTo("Hola Ana, tu pedido 55 fue confirmado.");
        // API-02 no lo expone (RN-06)
        assertThat(notificacionServices.consultarEstado(id).getMensaje()).isNull();
    }

    @Test
    void cu06PaginaLosResultados() {
        crearEmail(11L);
        crearEmail(11L);
        crearEmail(11L);

        PaginaResponseDto<NotificacionResponseDto> primera = historial(11L, null, null, null, 0, 2, null);
        PaginaResponseDto<NotificacionResponseDto> segunda = historial(11L, null, null, null, 1, 2, null);

        assertThat(primera.getContent()).hasSize(2);
        assertThat(primera.getSize()).isEqualTo(2);
        assertThat(primera.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(primera.getTotalPages()).isGreaterThanOrEqualTo(2);
        assertThat(segunda.getPage()).isEqualTo(1);
        assertThat(segunda.getContent()).extracting(NotificacionResponseDto::getId)
                .doesNotContainAnyElementsOf(primera.getContent().stream().map(NotificacionResponseDto::getId).toList());
    }

    @Test
    void cu06FiltraPorCanalEstadoYLectura() {
        Long email = crearEmail(11L);
        Long sinLeer = crearInApp(11L);
        Long leida = crearInApp(11L);
        notificacionServices.marcarComoLeida(leida);

        assertThat(historial(11L, null, "IN_APP", null, 0, 100, null).getContent())
                .extracting(NotificacionResponseDto::getCanal).containsOnly("IN_APP");
        assertThat(historial(11L, "enviada", "email", null, 0, 100, null).getContent())
                .extracting(NotificacionResponseDto::getId).contains(email).doesNotContain(sinLeer);
        assertThat(historial(11L, "FALLIDA_DEFINITIVA", null, null, 0, 100, null).getContent()).isEmpty();
        assertThat(historial(11L, null, "IN_APP", true, 0, 100, null).getContent())
                .extracting(NotificacionResponseDto::getId).contains(leida).doesNotContain(sinLeer);
        assertThat(historial(11L, null, "IN_APP", false, 0, 100, null).getContent())
                .extracting(NotificacionResponseDto::getId).contains(sinLeer).doesNotContain(leida);
    }

    @Test
    void cu06OrdenaPorElCampoIndicado() {
        crearEmail(11L);
        crearEmail(11L);

        List<Long> ascendente = historial(11L, null, null, null, 0, 100, "id,asc").getContent().stream()
                .map(NotificacionResponseDto::getId).toList();
        assertThat(ascendente).isSorted();
    }

    @Test
    void cu06UnUsuarioSinNotificacionesDevuelveUnaPaginaVacia() {
        PaginaResponseDto<NotificacionResponseDto> pagina = historial(9999L, null, null, null, 0, 20, null);

        assertThat(pagina.getContent()).isEmpty();
        assertThat(pagina.getTotalElements()).isZero();
    }

    @Test
    void cu06RechazaFiltrosYPaginacionInvalidos() {
        assertCodigo(() -> historial(11L, "ROTA", null, null, 0, 20, null), ValidacionException.class,
                "VALIDATION_ERROR");
        assertCodigo(() -> historial(11L, null, "SMS", null, 0, 20, null), ValidacionException.class,
                "VALIDATION_ERROR");
        assertCodigo(() -> historial(11L, null, null, null, -1, 20, null), ValidacionException.class,
                "VALIDATION_ERROR");
        assertCodigo(() -> historial(11L, null, null, null, 0, 0, null), ValidacionException.class,
                "VALIDATION_ERROR");
        assertCodigo(() -> historial(11L, null, null, null, 0, 101, null), ValidacionException.class,
                "VALIDATION_ERROR");
        assertCodigo(() -> historial(11L, null, null, null, 0, 20, "mensaje,asc"), ValidacionException.class,
                "VALIDATION_ERROR");
        assertCodigo(() -> historial(11L, null, null, null, 0, 20, "id,arriba"), ValidacionException.class,
                "VALIDATION_ERROR");
        assertCodigo(() -> historial(0L, null, null, null, 0, 20, null), ValidacionException.class,
                "VALIDATION_ERROR");
    }

    // ---------- CU-07 ----------

    private PlantillaRequestDto plantilla(String nombre, String cuerpo, String canal) {
        return new PlantillaRequestDto(nombre, "Asunto", cuerpo, null, canal);
    }

    @Test
    void cu07CreaUnaPlantillaActivaConSusVariables() {
        PlantillaResponseDto creada = plantillaServices.crearPlantilla(
                plantilla("ENTREGA_CONFIRMADA_EMAIL", "Hola {nombre}, entrega {codigo}. Gracias {nombre}", "EMAIL"));

        assertThat(creada.getId()).isNotNull();
        assertThat(creada.isActiva()).isTrue();
        assertThat(creada.getCanal()).isEqualTo("EMAIL");
        assertThat(creada.getVariables()).containsExactly("nombre", "codigo");
    }

    @Test
    void cu07LaPlantillaNuevaSePuedeUsarEnSeguida() {
        PlantillaResponseDto creada = plantillaServices.crearPlantilla(
                plantilla("PEDIDO_LISTO_IN_APP", "Tu pedido {pedidoId} está listo", "IN_APP"));

        NotificacionResponseDto notificacion = notificacionServices.crearNotificacion(
                new CrearNotificacionRequestDto(10L, creada.getId(), "IN_APP", Map.of("pedidoId", "7")));

        assertThat(notificacion.getEstado()).isEqualTo("ENVIADA");
        assertThat(notificacion.getPlantillaId()).isEqualTo(creada.getId());
    }

    @Test
    void cu07RechazaNombreDuplicadoYDatosInvalidos() {
        plantillaServices.crearPlantilla(plantilla("DUPLICADA_EMAIL", "Hola {nombre}", "EMAIL"));

        assertCodigo(() -> plantillaServices.crearPlantilla(plantilla("DUPLICADA_EMAIL", "Otro {nombre}", "PUSH")),
                RecursoDuplicadoException.class, "TEMPLATE_DUPLICATED");
        assertCodigo(() -> plantillaServices.crearPlantilla(plantilla(null, "Hola", "EMAIL")),
                ValidacionException.class, "VALIDATION_ERROR");
        assertCodigo(() -> plantillaServices.crearPlantilla(plantilla("SIN_CUERPO", " ", "EMAIL")),
                ValidacionException.class, "VALIDATION_ERROR");
        assertCodigo(() -> plantillaServices.crearPlantilla(plantilla("SIN_CANAL", "Hola", null)),
                ValidacionException.class, "VALIDATION_ERROR");
        assertCodigo(() -> plantillaServices.crearPlantilla(plantilla("CANAL_RARO", "Hola", "SMS")),
                ValidacionException.class, "VALIDATION_ERROR");
        assertCodigo(() -> plantillaServices.crearPlantilla(plantilla("MUY_LARGA", "x".repeat(1001), "EMAIL")),
                ValidacionException.class, "VALIDATION_ERROR");
    }

    @Test
    void cu07ModificaLaPlantillaSinAlterarLosMensajesYaGenerados() {
        PlantillaResponseDto creada = plantillaServices.crearPlantilla(
                plantilla("A_MODIFICAR_EMAIL", "Version uno {nombre}", "EMAIL"));
        Long notificacionId = notificacionServices.crearNotificacion(
                new CrearNotificacionRequestDto(11L, creada.getId(), "EMAIL", Map.of("nombre", "Bruno"))).getId();

        PlantillaResponseDto modificada = plantillaServices.modificarPlantilla(creada.getId(),
                new PlantillaRequestDto("A_MODIFICAR_EMAIL", "Nuevo asunto", "Version dos {nombre} {extra}", null,
                        "EMAIL"));

        assertThat(modificada.getCuerpo()).isEqualTo("Version dos {nombre} {extra}");
        assertThat(modificada.getVariables()).containsExactly("nombre", "extra");
        assertThat(modificada.isActiva()).isTrue();
        // RN-10: el mensaje ya generado no cambia
        assertThat(historial(11L, null, null, null, 0, 100, null).getContent().stream()
                .filter(n -> n.getId().equals(notificacionId)).findFirst().orElseThrow().getMensaje())
                .isEqualTo("Version uno Bruno");
        // y la plantilla modificada exige las variables nuevas (RN-08)
        assertCodigo(() -> notificacionServices.crearNotificacion(new CrearNotificacionRequestDto(11L, creada.getId(),
                "EMAIL", Map.of("nombre", "Bruno"))), ReglaNegocioException.class, "TEMPLATE_PARAMS_MISSING");
    }

    @Test
    void cu07UnaPlantillaDesactivadaDejaDeAceptarSolicitudes() {
        PlantillaResponseDto creada = plantillaServices.crearPlantilla(
                plantilla("A_DESACTIVAR_EMAIL", "Hola {nombre}", "EMAIL"));

        plantillaServices.modificarPlantilla(creada.getId(),
                new PlantillaRequestDto("A_DESACTIVAR_EMAIL", "Asunto", "Hola {nombre}", false, null));

        assertCodigo(() -> notificacionServices.crearNotificacion(new CrearNotificacionRequestDto(10L, creada.getId(),
                "EMAIL", Map.of("nombre", "Ana"))), ReglaNegocioException.class, "TEMPLATE_INVALID");
        assertThat(plantillaServices.consultarPlantilla(creada.getId()).isActiva()).isFalse();
    }

    @Test
    void cu07NoPermiteCambiarElCanalNiRepetirElNombreDeOtra() {
        PlantillaResponseDto creada = plantillaServices.crearPlantilla(
                plantilla("CANAL_FIJO_PUSH", "Hola {nombre}", "PUSH"));

        assertCodigo(() -> plantillaServices.modificarPlantilla(creada.getId(),
                new PlantillaRequestDto("CANAL_FIJO_PUSH", "Asunto", "Hola {nombre}", null, "EMAIL")),
                ReglaNegocioException.class, "TEMPLATE_CHANNEL_IMMUTABLE");
        assertCodigo(() -> plantillaServices.modificarPlantilla(creada.getId(),
                new PlantillaRequestDto("PEDIDO_CONFIRMADO_EMAIL", "Asunto", "Hola {nombre}", null, "PUSH")),
                RecursoDuplicadoException.class, "TEMPLATE_DUPLICATED");
        // Guardarla con su propio nombre no es un duplicado
        assertThat(plantillaServices.modificarPlantilla(creada.getId(),
                new PlantillaRequestDto("CANAL_FIJO_PUSH", "Asunto", "Chau {nombre}", null, "PUSH")).getCuerpo())
                .isEqualTo("Chau {nombre}");
    }

    @Test
    void cu07ConsultaYListaPlantillas() {
        PlantillaResponseDto creada = plantillaServices.crearPlantilla(
                plantilla("PARA_LISTAR_EMAIL", "Hola {nombre}", "EMAIL"));

        assertThat(plantillaServices.consultarPlantilla(creada.getId()).getNombre()).isEqualTo("PARA_LISTAR_EMAIL");
        assertThat(plantillaServices.listarPlantillas()).extracting(PlantillaResponseDto::getNombre)
                .contains("PEDIDO_CONFIRMADO_EMAIL", "PAGO_APROBADO_PUSH", "ENVIO_EN_CAMINO_IN_APP",
                        "PARA_LISTAR_EMAIL");
        assertCodigo(() -> plantillaServices.consultarPlantilla(99999L), RecursoNoEncontradoException.class,
                "TEMPLATE_NOT_FOUND");
        assertCodigo(() -> plantillaServices.modificarPlantilla(99999L, plantilla("X_EMAIL", "Hola", "EMAIL")),
                RecursoNoEncontradoException.class, "TEMPLATE_NOT_FOUND");
    }

    // ---------- CU-08 (selección del canal) ----------

    @Test
    void cu08SinCanalUsaElDeLaPlantilla() {
        NotificacionResponseDto email = notificacionServices.crearNotificacion(
                new CrearNotificacionRequestDto(10L, 1L, null, Map.of("nombre", "Ana", "pedidoId", "1")));
        NotificacionResponseDto push = notificacionServices.crearNotificacion(
                new CrearNotificacionRequestDto(10L, 2L, " ", Map.of("monto", "100")));
        NotificacionResponseDto inApp = notificacionServices.crearNotificacion(
                new CrearNotificacionRequestDto(10L, 3L, null, Map.of("codigo", "Z")));

        assertThat(email.getCanal()).isEqualTo("EMAIL");
        assertThat(push.getCanal()).isEqualTo("PUSH");
        assertThat(inApp.getCanal()).isEqualTo("IN_APP");
    }

    @Test
    void cu08UnCanalDistintoAlDeLaPlantillaSeRechaza() {
        assertCodigo(() -> notificacionServices.crearNotificacion(new CrearNotificacionRequestDto(10L, 1L, "PUSH",
                Map.of("nombre", "Ana", "pedidoId", "1"))), ReglaNegocioException.class, "TEMPLATE_INVALID");
    }

    @Test
    void cu08SinCanalYConPlantillaInexistenteDevuelveTemplateInvalid() {
        assertCodigo(() -> notificacionServices.crearNotificacion(new CrearNotificacionRequestDto(10L, 9999L, null,
                Map.of())), ReglaNegocioException.class, "TEMPLATE_INVALID");
    }

    // ---------- CU-10 ----------

    @Test
    void cu10MarcaLaNotificacionInternaComoLeida() {
        Long id = crearInApp(10L);
        assertThat(notificacionServices.consultarEstado(id).getFechaLectura()).isNull();

        NotificacionResponseDto leida = notificacionServices.marcarComoLeida(id);

        assertThat(leida.getFechaLectura()).isNotNull();
        assertThat(leida.getEstado()).isEqualTo("ENVIADA");
        assertThat(notificacionServices.consultarEstado(id).getFechaLectura()).isEqualTo(leida.getFechaLectura());
    }

    @Test
    void cu10EsIdempotenteYConservaLaFechaDeLaPrimeraLectura() {
        Long id = crearInApp(10L);

        NotificacionResponseDto primera = notificacionServices.marcarComoLeida(id);
        NotificacionResponseDto segunda = notificacionServices.marcarComoLeida(id);

        assertThat(segunda.getFechaLectura()).isEqualTo(primera.getFechaLectura());
    }

    @Test
    void cu10SoloLasNotificacionesInAppPuedenLeerse() {
        Long email = crearEmail(10L);

        assertCodigo(() -> notificacionServices.marcarComoLeida(email), ReglaNegocioException.class,
                "NOTIFICATION_NOT_IN_APP");
        assertThat(notificacionServices.consultarEstado(email).getFechaLectura()).isNull();
    }

    @Test
    void cu10UnaNotificacionNoEntregadaNoPuedeLeerse() {
        Long id = crearInApp(10L);
        // El emisor IN_APP no falla: se fuerza el estado para representar una notificación que no llegó a entregarse
        Notificacion notificacion = notificacionRepository.findById(id).orElseThrow();
        ReflectionTestUtils.setField(notificacion, "estado", EstadoNotificacion.FALLIDA);
        notificacionRepository.save(notificacion);

        assertCodigo(() -> notificacionServices.marcarComoLeida(id), EstadoInvalidoException.class,
                "NOTIFICATION_NOT_DELIVERED");
        assertThat(notificacionServices.consultarEstado(id).getFechaLectura()).isNull();
    }

    @Test
    void cu10FallaConNotificacionInexistente() {
        assertCodigo(() -> notificacionServices.marcarComoLeida(99999L), RecursoNoEncontradoException.class,
                "NOTIFICATION_NOT_FOUND");
    }

    // ---------- CU-09 ----------

    @Test
    void cu09ListaElIntentoExitosoSinDetalleDeError() {
        Long id = crearEmail(10L);

        List<IntentoEnvioResponseDto> intentos = notificacionServices.consultarIntentos(id);

        assertThat(intentos).hasSize(1);
        assertThat(intentos.get(0).getNumeroIntento()).isEqualTo(1);
        assertThat(intentos.get(0).getResultado()).isEqualTo("EXITOSO");
        assertThat(intentos.get(0).getDetalleError()).isNull();
        assertThat(intentos.get(0).getFecha()).isNotNull();
    }

    @Test
    void cu09FallaConNotificacionInexistente() {
        assertCodigo(() -> notificacionServices.consultarIntentos(99999L), RecursoNoEncontradoException.class,
                "NOTIFICATION_NOT_FOUND");
    }
}
