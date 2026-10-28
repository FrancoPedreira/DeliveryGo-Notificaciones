package notificaciones.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import notificaciones.dto.CrearNotificacionRequestDto;
import notificaciones.exception.ServicioNoDisponibleException;
import notificaciones.repository.NotificacionRepository;

// CU-01 A7 y DM-10: si Usuarios no responde, la solicitud se rechaza con 503 y no se registra
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "notificaciones.usuarios.simular-caida=true")
class UsuariosNoDisponibleTest {

    @Autowired
    private NotificacionServices notificacionServices;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rechazaLaSolicitudSinRegistrarla() {
        long antes = notificacionRepository.count();

        assertThatThrownBy(() -> notificacionServices.crearNotificacion(
                new CrearNotificacionRequestDto(10L, 3L, "IN_APP", Map.of("codigo", "X1"))))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .extracting("code").isEqualTo("USERS_SERVICE_UNAVAILABLE");

        assertThat(notificacionRepository.count()).isEqualTo(antes);
    }

    @Test
    void api01Devuelve503() throws Exception {
        mockMvc.perform(post("/api/v1/notifications").contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinatarioId\":10,\"plantillaId\":3,\"canal\":\"IN_APP\",\"parametros\":{\"codigo\":\"X1\"}}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("USERS_SERVICE_UNAVAILABLE"));
    }
}
