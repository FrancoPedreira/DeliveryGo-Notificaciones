package notificaciones.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

// X-Correlation-Id: CorrelationIdFilter lo devuelve en todas las respuestas y el error usa el mismo valor
@SpringBootTest
@AutoConfigureMockMvc
class CorrelationIdControllerTest {
    private static final String HEADER = "X-Correlation-Id";
    private static final String UUID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void respuestaExitosaDevuelveElMismoCorrelationIdQueEnvioElLlamador() throws Exception {
        mockMvc.perform(get("/api/v1/channels").header(HEADER, "pedido-123"))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER, "pedido-123"));
    }

    @Test
    void respuestaExitosaSinHeaderDevuelveUnCorrelationIdGenerado() throws Exception {
        mockMvc.perform(get("/api/v1/channels"))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER, matchesPattern(UUID_REGEX)));
    }

    @Test
    void headerEnBlancoSeTrataComoSiNoViniera() throws Exception {
        mockMvc.perform(get("/api/v1/channels").header(HEADER, "  "))
                .andExpect(header().string(HEADER, matchesPattern(UUID_REGEX)));
    }

    @Test
    void errorConHeaderCoincideConElDelBody() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/999999").header(HEADER, "pedido-123"))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HEADER, "pedido-123"))
                .andExpect(jsonPath("$.correlationId").value("pedido-123"));
    }

    @Test
    void errorSinHeaderGeneraUnoYCoincideEnHeaderYBody() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v1/notifications/999999"))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HEADER, matchesPattern(UUID_REGEX)))
                .andReturn();

        String delHeader = resultado.getResponse().getHeader(HEADER);
        String delBody = JsonPath.read(resultado.getResponse().getContentAsString(), "$.correlationId");
        org.junit.jupiter.api.Assertions.assertEquals(delHeader, delBody);
    }

    @Test
    void errorDeRutaInexistenteTambienDevuelveElHeader() throws Exception {
        mockMvc.perform(get("/api/v1/no-existe").header(HEADER, "pedido-123"))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HEADER, "pedido-123"))
                .andExpect(jsonPath("$.correlationId").value("pedido-123"));
    }
}
