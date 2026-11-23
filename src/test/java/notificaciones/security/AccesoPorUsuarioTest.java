package notificaciones.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.Filter;

// Autorización sobre el recurso: cada persona solo ve y modifica sus propias notificaciones; ADMIN, todas.
// Usuarios simulados: 10 (Ana) y 11 (Bruno). Arma su propio MockMvc para no heredar el token ADMIN por defecto
@SpringBootTest
class AccesoPorUsuarioTest {
    private static final long ANA = 10L;
    private static final long BRUNO = 11L;

    @Autowired
    private WebApplicationContext contexto;
    @Autowired
    private TokenDePrueba tokens;

    private MockMvc mockMvc;
    private long notificacionDeBruno;

    @BeforeEach
    void preparar() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto)
                .addFilters(contexto.getBean("correlationIdFilter", Filter.class),
                        contexto.getBean("springSecurityFilterChain", Filter.class))
                .build();

        // Plantilla 4 (bienvenida, EMAIL). El alta no pide token: la hacen otros módulos
        String respuesta = mockMvc.perform(post("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinatarioId\":" + BRUNO + ",\"plantillaId\":4,\"parametros\":{\"nombre\":\"Bruno\"}}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        notificacionDeBruno = ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private String token(long usuarioId, String rol) {
        return "Bearer " + tokens.crear(usuarioId, rol);
    }

    @Test
    void cadaUsuarioConsultaSuPropioHistorial() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + BRUNO + "/notifications").header("Authorization", token(BRUNO, "CLIENTE")))
                .andExpect(status().isOk());
    }

    @Test
    void elHistorialDeOtroUsuarioDevuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + BRUNO + "/notifications").header("Authorization", token(ANA, "CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminConsultaElHistorialDeCualquierUsuario() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + BRUNO + "/notifications").header("Authorization", token(1L, "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void elDestinatarioConsultaSuNotificacionYSusIntentos() throws Exception {
        String bruno = token(BRUNO, "CLIENTE");
        mockMvc.perform(get("/api/v1/notifications/" + notificacionDeBruno).header("Authorization", bruno))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/notifications/" + notificacionDeBruno + "/attempts").header("Authorization", bruno))
                .andExpect(status().isOk());
    }

    // Consultar, ver intentos, reintentar y marcar como leída una notificación ajena: 403 antes de tocar nada
    @Test
    void operarSobreLaNotificacionDeOtroUsuarioDevuelve403() throws Exception {
        String ana = token(ANA, "CLIENTE");
        String base = "/api/v1/notifications/" + notificacionDeBruno;
        mockMvc.perform(get(base).header("Authorization", ana))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get(base + "/attempts").header("Authorization", ana))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(base + "/retry").header("Authorization", ana))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(base + "/read").header("Authorization", ana))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminConsultaCualquierNotificacion() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/" + notificacionDeBruno).header("Authorization", token(1L, "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void unaNotificacionInexistenteSigueDevolviendo404() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/999999").header("Authorization", token(ANA, "CLIENTE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
    }
}
