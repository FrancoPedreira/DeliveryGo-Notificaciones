package notificaciones.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import notificaciones.dto.CanalDto;
import notificaciones.dto.CrearNotificacionRequestDto;
import notificaciones.exception.NotificacionesException;
import notificaciones.exception.RecursoNoEncontradoException;
import notificaciones.exception.ReglaNegocioException;
import notificaciones.exception.ValidacionException;

// CU-08 y RN-07: habilitar y deshabilitar canales. DirtiesContext: deshabilita canales, así que este contexto
// no se reutiliza en otros tests
@SpringBootTest
@DirtiesContext
@TestPropertySource(properties = "notificaciones.emisor.tasa-fallo=0")
class CanalAdministracionTest {

    @Autowired
    private CanalServices canalServices;

    @Autowired
    private NotificacionServices notificacionServices;

    private void assertCodigo(Runnable accion, Class<? extends NotificacionesException> tipo, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOf(tipo).extracting("code").isEqualTo(codigo);
    }

    @Test
    void cu08ListaLosTresCanalesHabilitados() {
        List<CanalDto> canales = canalServices.listarCanales();

        assertThat(canales).extracting(CanalDto::getTipo).containsExactlyInAnyOrder("EMAIL", "PUSH", "IN_APP");
        assertThat(canales).extracting(CanalDto::isActivo).containsOnly(true);
    }

    @Test
    void cu08UnCanalDeshabilitadoRechazaSolicitudesNuevasHastaQueSeHabilitaDeNuevo() {
        assertThat(canalServices.cambiarEstadoCanal("PUSH", false).isActivo()).isFalse();

        // Con canal explícito y también tomando el canal de la plantilla (RN-13)
        assertCodigo(() -> notificacionServices.crearNotificacion(new CrearNotificacionRequestDto(10L, 2L, "PUSH",
                Map.of("monto", "5"))), ReglaNegocioException.class, "CHANNEL_INACTIVE");
        assertCodigo(() -> notificacionServices.crearNotificacion(new CrearNotificacionRequestDto(10L, 2L, null,
                Map.of("monto", "5"))), ReglaNegocioException.class, "CHANNEL_INACTIVE");
        // Los otros canales siguen funcionando
        assertThat(notificacionServices.crearNotificacion(new CrearNotificacionRequestDto(10L, 3L, null,
                Map.of("codigo", "Z"))).getEstado()).isEqualTo("ENVIADA");

        assertThat(canalServices.cambiarEstadoCanal("push", true).isActivo()).isTrue();
        assertThat(notificacionServices.crearNotificacion(new CrearNotificacionRequestDto(10L, 2L, null,
                Map.of("monto", "5"))).getEstado()).isEqualTo("ENVIADA");
    }

    @Test
    void cu08CambiarElEstadoEsIdempotente() {
        canalServices.cambiarEstadoCanal("EMAIL", true);

        assertThat(canalServices.cambiarEstadoCanal("EMAIL", true).isActivo()).isTrue();
    }

    @Test
    void cu08RechazaCanalInexistenteYEstadoFaltante() {
        assertCodigo(() -> canalServices.cambiarEstadoCanal("SMS", true), RecursoNoEncontradoException.class,
                "CHANNEL_NOT_FOUND");
        assertCodigo(() -> canalServices.cambiarEstadoCanal("EMAIL", null), ValidacionException.class,
                "VALIDATION_ERROR");
    }
}
