package notificaciones.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

// API-01 y API-03 por HTTP con EMAIL siempre fallando: 201 FALLIDA, reintentos 200 y 409 al agotarse
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = { "notificaciones.emisor.tasa-fallo=1", "notificaciones.max-intentos=3" })
class ReintentoControllerTest {
    private static final String URL = "/api/v1/notifications";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void cicloCompletoDeUnaNotificacionFallida() throws Exception {
        MvcResult creada = mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"destinatarioId":10,"plantillaId":1,"canal":"EMAIL","parametros":{"nombre":"Ana","pedidoId":"55"}}"""))
                // CU-01 A5: el fallo de envío no es un error de la solicitud
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("FALLIDA"))
                .andExpect(jsonPath("$.cantidadIntentos").value(1))
                .andReturn();
        long id = ((Number) JsonPath.read(creada.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(post(URL + "/" + id + "/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FALLIDA"))
                .andExpect(jsonPath("$.cantidadIntentos").value(2));
        mockMvc.perform(post(URL + "/" + id + "/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FALLIDA_DEFINITIVA"))
                .andExpect(jsonPath("$.cantidadIntentos").value(3));
        mockMvc.perform(post(URL + "/" + id + "/retry"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MAX_RETRIES_EXCEEDED"));
    }
}
