package notificaciones.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import notificaciones.dto.CrearNotificacionRequestDto;
import notificaciones.dto.IntentoEnvioResponseDto;

// CU-09 con el envío EMAIL siempre fallido: el historial de intentos refleja cada reintento hasta el máximo
@SpringBootTest
@TestPropertySource(properties = { "notificaciones.emisor.tasa-fallo=1", "notificaciones.max-intentos=3" })
class IntentosEnvioTest {

    @Autowired
    private NotificacionServices notificacionServices;

    @Test
    void cu09ListaTodosLosIntentosEnOrdenConElMotivoDelFallo() {
        Long id = notificacionServices.crearNotificacion(new CrearNotificacionRequestDto(10L, 1L, "EMAIL",
                Map.of("nombre", "Ana", "pedidoId", "55"))).getId();
        notificacionServices.reintentarNotificacion(id);
        assertThat(notificacionServices.consultarIntentos(id)).hasSize(2);
        notificacionServices.reintentarNotificacion(id);

        List<IntentoEnvioResponseDto> intentos = notificacionServices.consultarIntentos(id);

        assertThat(intentos).extracting(IntentoEnvioResponseDto::getNumeroIntento).containsExactly(1, 2, 3);
        assertThat(intentos).extracting(IntentoEnvioResponseDto::getResultado).containsOnly("FALLIDO");
        assertThat(intentos).extracting(IntentoEnvioResponseDto::getDetalleError)
                .containsOnly("Fallo simulado del proveedor EMAIL");
        // La cantidad del contador (DM-08) coincide con los intentos registrados
        assertThat(notificacionServices.consultarEstado(id).getCantidadIntentos()).isEqualTo(intentos.size());
        assertThat(notificacionServices.consultarEstado(id).getEstado()).isEqualTo("FALLIDA_DEFINITIVA");
    }
}
