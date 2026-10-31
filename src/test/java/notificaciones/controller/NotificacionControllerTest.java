package notificaciones.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

// API-01 a API-03 por HTTP con envío siempre exitoso: status, cuerpo (DTO-02 / DTO-03) y headers
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "notificaciones.emisor.tasa-fallo=0")
class NotificacionControllerTest {
    private static final String URL = "/api/v1/notifications";
    private static final String SOLICITUD_OK = """
            {"destinatarioId":10,"plantillaId":1,"canal":"EMAIL","parametros":{"nombre":"Ana","pedidoId":"55"}}""";

    @Autowired
    private MockMvc mockMvc;

    private Long crear(String body) throws Exception {
        MvcResult resultado = mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void api01Crea201ConElDto02() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(SOLICITUD_OK))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.destinatarioId").value(10))
                .andExpect(jsonPath("$.plantillaId").value(1))
                .andExpect(jsonPath("$.canal").value("EMAIL"))
                .andExpect(jsonPath("$.estado").value("ENVIADA"))
                .andExpect(jsonPath("$.cantidadIntentos").value(1))
                // RN-06: la respuesta no expone el mensaje ni los parámetros
                .andExpect(jsonPath("$.mensaje").doesNotExist())
                .andExpect(jsonPath("$.parametros").doesNotExist());
    }

    @Test
    void api01Devuelve400ConElFormatoDeErrorYElCorrelationIdDelLlamador() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).header("X-Correlation-Id", "pedido-123")
                .content("{\"destinatarioId\":10,\"canal\":\"EMAIL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Correlation-Id", "pedido-123"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value(URL))
                .andExpect(jsonPath("$.correlationId").value("pedido-123"))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }

    @Test
    void api01Devuelve400SiElJsonEstaMalFormado() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void api01Devuelve422PorReglasDeNegocio() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinatarioId\":10,\"plantillaId\":1,\"canal\":\"SMS\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CHANNEL_INACTIVE"));
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinatarioId\":999,\"plantillaId\":3,\"canal\":\"IN_APP\",\"parametros\":{\"codigo\":\"X\"}}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("RECIPIENT_NOT_FOUND"));
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinatarioId\":11,\"plantillaId\":2,\"canal\":\"PUSH\",\"parametros\":{\"monto\":\"1\"}}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("RECIPIENT_CONTACT_MISSING"));
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinatarioId\":10,\"plantillaId\":1,\"canal\":\"PUSH\",\"parametros\":{\"monto\":\"1\"}}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("TEMPLATE_INVALID"));
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinatarioId\":10,\"plantillaId\":1,\"canal\":\"EMAIL\",\"parametros\":{\"nombre\":\"Ana\"}}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("TEMPLATE_PARAMS_MISSING"));
    }

    @Test
    void api02Consulta200() throws Exception {
        Long id = crear(SOLICITUD_OK);

        mockMvc.perform(get(URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.estado").value("ENVIADA"));
    }

    @Test
    void api02Devuelve404Y400() throws Exception {
        mockMvc.perform(get(URL + "/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
        mockMvc.perform(get(URL + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void api03Devuelve409SiNoEstaFallidaY404SiNoExiste() throws Exception {
        Long id = crear(SOLICITUD_OK);

        mockMvc.perform(post(URL + "/" + id + "/retry"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_RETRYABLE"));
        mockMvc.perform(post(URL + "/99999/retry"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
    }

    @Test
    void swaggerPublicaElContrato() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/v1/notifications/{id}/retry")))
                .andExpect(content().string(containsString("CrearNotificacionRequestDto")))
                .andExpect(content().string(containsString("TEMPLATE_PARAMS_MISSING")));
    }
}
