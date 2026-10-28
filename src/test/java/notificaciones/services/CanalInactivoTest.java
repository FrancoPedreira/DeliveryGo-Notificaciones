package notificaciones.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Comparator;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import notificaciones.dto.CrearNotificacionRequestDto;
import notificaciones.exception.ReglaNegocioException;
import notificaciones.model.Canal;
import notificaciones.model.IntentoEnvio;
import notificaciones.model.ResultadoIntento;
import notificaciones.model.TipoCanal;
import notificaciones.repository.CanalRepository;
import notificaciones.repository.IntentoEnvioRepository;

// CU-03 A3 y RN-07: un canal desactivado después de crear la notificación hace fallar el intento, sin excepción.
// DirtiesContext: desactiva EMAIL, así que este contexto no se reutiliza en otros tests
@SpringBootTest
@DirtiesContext
@TestPropertySource(properties = { "notificaciones.emisor.tasa-fallo=1", "notificaciones.max-intentos=3" })
class CanalInactivoTest {

    @Autowired
    private NotificacionServices notificacionServices;

    @Autowired
    private CanalRepository canalRepository;

    @Autowired
    private IntentoEnvioRepository intentoEnvioRepository;

    @Test
    void elReintentoConElCanalInactivoRegistraUnIntentoFallido() {
        Long id = notificacionServices.crearNotificacion(new CrearNotificacionRequestDto(10L, 1L, "EMAIL",
                Map.of("nombre", "Ana", "pedidoId", "55"))).getId();

        Canal email = canalRepository.findByTipo(TipoCanal.EMAIL).orElseThrow();
        email.desactivar();
        canalRepository.save(email);

        // El reintento no lanza excepción: el intento queda FALLIDO con el motivo
        assertThat(notificacionServices.reintentarNotificacion(id).getEstado()).isEqualTo("FALLIDA");

        IntentoEnvio ultimo = intentoEnvioRepository.findAll().stream()
                .filter(intento -> intento.getNotificacion().getId().equals(id))
                .max(Comparator.comparingInt(IntentoEnvio::getNumeroIntento))
                .orElseThrow();
        assertThat(ultimo.getNumeroIntento()).isEqualTo(2);
        assertThat(ultimo.getResultado()).isEqualTo(ResultadoIntento.FALLIDO);
        assertThat(ultimo.getDetalleError()).isEqualTo("El canal EMAIL está inactivo");

        // RN-07 en CU-01: con el canal inactivo ya no se aceptan solicitudes nuevas
        assertThatThrownBy(() -> notificacionServices.crearNotificacion(new CrearNotificacionRequestDto(10L, 1L,
                "EMAIL", Map.of("nombre", "Ana", "pedidoId", "55"))))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("code").isEqualTo("CHANNEL_INACTIVE");
    }
}
