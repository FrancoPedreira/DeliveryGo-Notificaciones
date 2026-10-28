package notificaciones.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import notificaciones.dto.CrearNotificacionRequestDto;
import notificaciones.exception.EstadoInvalidoException;
import notificaciones.model.IntentoEnvio;
import notificaciones.repository.IntentoEnvioRepository;

// EMAIL siempre falla: prueba CU-03 A1/A2, CU-05, RN-04, DM-08 y el bloqueo de DM-09
@SpringBootTest
@TestPropertySource(properties = { "notificaciones.emisor.tasa-fallo=1", "notificaciones.max-intentos=3" })
class ReintentoNotificacionTest {

    @Autowired
    private NotificacionServices notificacionServices;

    @Autowired
    private IntentoEnvioRepository intentoEnvioRepository;

    private Long crearNotificacionFallida() {
        return notificacionServices.crearNotificacion(new CrearNotificacionRequestDto(10L, 1L, "EMAIL",
                Map.of("nombre", "Ana", "pedidoId", "55"))).getId();
    }

    private List<Integer> numerosDeIntento(Long notificacionId) {
        return intentoEnvioRepository.findAll().stream()
                .filter(intento -> intento.getNotificacion().getId().equals(notificacionId))
                .map(IntentoEnvio::getNumeroIntento)
                .sorted()
                .toList();
    }

    @Test
    void falloDeEnvioSeRegistraComoFallidaYSeAgotaAlMaximo() {
        Long id = crearNotificacionFallida();
        // CU-01 A5: la notificación se registra igual, en FALLIDA
        assertThat(notificacionServices.consultarEstado(id).getEstado()).isEqualTo("FALLIDA");

        assertThat(notificacionServices.reintentarNotificacion(id).getEstado()).isEqualTo("FALLIDA");
        // RN-04: al tercer intento queda FALLIDA_DEFINITIVA
        assertThat(notificacionServices.reintentarNotificacion(id).getEstado()).isEqualTo("FALLIDA_DEFINITIVA");

        assertThatThrownBy(() -> notificacionServices.reintentarNotificacion(id))
                .isInstanceOf(EstadoInvalidoException.class)
                .extracting("code").isEqualTo("MAX_RETRIES_EXCEEDED");

        // DM-08: el contador coincide con la cantidad de IntentoEnvio y con el mayor numeroIntento
        assertThat(notificacionServices.consultarEstado(id).getCantidadIntentos()).isEqualTo(3);
        assertThat(numerosDeIntento(id)).containsExactly(1, 2, 3);
    }

    @Test
    void reintentosSimultaneosSeProcesanDeAUno() throws Exception {
        Long id = crearNotificacionFallida();
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Future<String> primero = pool.submit(() -> {
            largada.await();
            return notificacionServices.reintentarNotificacion(id).getEstado();
        });
        Future<String> segundo = pool.submit(() -> {
            largada.await();
            return notificacionServices.reintentarNotificacion(id).getEstado();
        });
        largada.countDown();

        // CU-05 A4: el bloqueo de REP-07 hace que el segundo espere y vea el estado que dejó el primero,
        // así que se registran los intentos 2 y 3 sin repetir números
        assertThat(List.of(primero.get(), segundo.get())).containsExactlyInAnyOrder("FALLIDA", "FALLIDA_DEFINITIVA");
        assertThat(numerosDeIntento(id)).containsExactly(1, 2, 3);
        pool.shutdown();
    }
}
