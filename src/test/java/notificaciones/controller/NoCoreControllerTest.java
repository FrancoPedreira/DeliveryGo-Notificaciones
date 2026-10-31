package notificaciones.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

// API-04 a API-12 (CU-06 a CU-10) por HTTP con envío siempre exitoso: status, cuerpo y errores en formato DTO-03
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "notificaciones.emisor.tasa-fallo=0")
class NoCoreControllerTest {
    private static final String NOTIFICACIONES = "/api/v1/notifications";
    private static final String PLANTILLAS = "/api/v1/templates";

    @Autowired
    private MockMvc mockMvc;

    private Long crearNotificacion(String body) throws Exception {
        MvcResult resultado = mockMvc.perform(post(NOTIFICACIONES).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private Long crearInApp(long destinatarioId) throws Exception {
        return crearNotificacion("{\"destinatarioId\":" + destinatarioId
                + ",\"plantillaId\":3,\"parametros\":{\"codigo\":\"X1\"}}");
    }

    // ---------- API-04 (CU-06) ----------

    @Test
    void api04DevuelveElHistorialPaginadoConElMensaje() throws Exception {
        Long id = crearNotificacion(
                "{\"destinatarioId\":10,\"plantillaId\":1,\"canal\":\"EMAIL\",\"parametros\":{\"nombre\":\"Ana\",\"pedidoId\":\"77\"}}");

        mockMvc.perform(get("/api/v1/users/10/notifications").param("size", "100").param("canal", "EMAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.totalElements").value(notNullValue()))
                .andExpect(jsonPath("$.totalPages").value(notNullValue()))
                .andExpect(jsonPath("$.content[*].id", hasItem(id.intValue())))
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")].mensaje",
                        hasItem("Hola Ana, tu pedido 77 fue confirmado.")))
                // RN-06: los parámetros nunca se devuelven
                .andExpect(jsonPath("$.content[0].parametros").doesNotExist());
    }

    @Test
    void api04UsuarioSinNotificacionesDevuelve200ConPaginaVacia() throws Exception {
        mockMvc.perform(get("/api/v1/users/9999/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void api04Devuelve400ConFiltrosOPaginacionInvalidos() throws Exception {
        mockMvc.perform(get("/api/v1/users/10/notifications").param("estado", "ROTA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/10/notifications"));
        mockMvc.perform(get("/api/v1/users/10/notifications").param("size", "1000"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/users/10/notifications").param("sort", "mensaje,asc"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/users/10/notifications").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        mockMvc.perform(get("/api/v1/users/abc/notifications"))
                .andExpect(status().isBadRequest());
    }

    // ---------- API-05 a API-08 (CU-07) ----------

    @Test
    void api05Crea201ConLasVariablesYApi07LaConsulta() throws Exception {
        MvcResult resultado = mockMvc.perform(post(PLANTILLAS).contentType(MediaType.APPLICATION_JSON).content(
                "{\"nombre\":\"CTRL_PAGO_RECHAZADO_EMAIL\",\"asunto\":\"Pago rechazado\",\"cuerpo\":\"Hola {nombre}, rechazamos {monto}\",\"canal\":\"EMAIL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.activa").value(true))
                .andExpect(jsonPath("$.canal").value("EMAIL"))
                .andExpect(jsonPath("$.variables[0]").value("nombre"))
                .andExpect(jsonPath("$.variables[1]").value("monto"))
                .andReturn();
        Long id = ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(get(PLANTILLAS + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("CTRL_PAGO_RECHAZADO_EMAIL"));
        mockMvc.perform(get(PLANTILLAS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("CTRL_PAGO_RECHAZADO_EMAIL")));
    }

    @Test
    void api05Devuelve409SiElNombreYaExisteY400SiFaltanDatos() throws Exception {
        mockMvc.perform(post(PLANTILLAS).contentType(MediaType.APPLICATION_JSON).content(
                "{\"nombre\":\"PEDIDO_CONFIRMADO_EMAIL\",\"asunto\":\"A\",\"cuerpo\":\"B\",\"canal\":\"EMAIL\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TEMPLATE_DUPLICATED"));
        mockMvc.perform(post(PLANTILLAS).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"CTRL_SIN_CUERPO\",\"asunto\":\"A\",\"canal\":\"EMAIL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void api06Modifica200Y404Y422() throws Exception {
        MvcResult resultado = mockMvc.perform(post(PLANTILLAS).contentType(MediaType.APPLICATION_JSON).content(
                "{\"nombre\":\"CTRL_A_MODIFICAR_PUSH\",\"asunto\":\"A\",\"cuerpo\":\"Hola {nombre}\",\"canal\":\"PUSH\"}"))
                .andExpect(status().isCreated()).andReturn();
        Long id = ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(put(PLANTILLAS + "/" + id).contentType(MediaType.APPLICATION_JSON).content(
                "{\"nombre\":\"CTRL_A_MODIFICAR_PUSH\",\"asunto\":\"B\",\"cuerpo\":\"Chau {nombre}\",\"activa\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuerpo").value("Chau {nombre}"))
                .andExpect(jsonPath("$.activa").value(false));
        mockMvc.perform(put(PLANTILLAS + "/" + id).contentType(MediaType.APPLICATION_JSON).content(
                "{\"nombre\":\"CTRL_A_MODIFICAR_PUSH\",\"asunto\":\"B\",\"cuerpo\":\"Chau\",\"canal\":\"EMAIL\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("TEMPLATE_CHANNEL_IMMUTABLE"));
        mockMvc.perform(put(PLANTILLAS + "/99999").contentType(MediaType.APPLICATION_JSON).content(
                "{\"nombre\":\"X_NO_EXISTE\",\"asunto\":\"B\",\"cuerpo\":\"Chau\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEMPLATE_NOT_FOUND"));
        mockMvc.perform(get(PLANTILLAS + "/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEMPLATE_NOT_FOUND"));
    }

    // ---------- API-09 y API-10 (CU-08) ----------

    @Test
    void api01SinCanalUsaElDeLaPlantilla() throws Exception {
        mockMvc.perform(post(NOTIFICACIONES).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinatarioId\":10,\"plantillaId\":2,\"parametros\":{\"monto\":\"900\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.canal").value("PUSH"));
    }

    @Test
    void api09ListaLosCanales() throws Exception {
        mockMvc.perform(get("/api/v1/channels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipo", hasItem("EMAIL")))
                .andExpect(jsonPath("$[*].tipo", hasItem("PUSH")))
                .andExpect(jsonPath("$[*].tipo", hasItem("IN_APP")));
    }

    @Test
    void api10Devuelve404ConCanalInexistenteY400SinElCampoActivo() throws Exception {
        mockMvc.perform(patch("/api/v1/channels/SMS").contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHANNEL_NOT_FOUND"));
        mockMvc.perform(patch("/api/v1/channels/EMAIL").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        // Habilitar un canal ya habilitado no cambia nada (no se deshabilita ninguno: comparte contexto con otros tests)
        mockMvc.perform(patch("/api/v1/channels/email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("EMAIL"))
                .andExpect(jsonPath("$.activo").value(true));
    }

    // ---------- API-11 (CU-09) ----------

    @Test
    void api11ListaLosIntentosYDevuelve404SiNoExiste() throws Exception {
        Long id = crearInApp(10);

        mockMvc.perform(get(NOTIFICACIONES + "/" + id + "/attempts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numeroIntento").value(1))
                .andExpect(jsonPath("$[0].resultado").value("EXITOSO"))
                .andExpect(jsonPath("$[0].fecha").value(notNullValue()))
                .andExpect(jsonPath("$[0].detalleError").doesNotExist());
        mockMvc.perform(get(NOTIFICACIONES + "/99999/attempts"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
    }

    // ---------- API-12 (CU-10) ----------

    @Test
    void api12MarcaComoLeidaDeFormaIdempotente() throws Exception {
        Long id = crearInApp(10);

        MvcResult primera = mockMvc.perform(patch(NOTIFICACIONES + "/" + id + "/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENVIADA"))
                .andExpect(jsonPath("$.fechaLectura").value(notNullValue()))
                .andReturn();
        String fechaLectura = JsonPath.read(primera.getResponse().getContentAsString(), "$.fechaLectura");

        mockMvc.perform(patch(NOTIFICACIONES + "/" + id + "/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaLectura").value(fechaLectura));
        mockMvc.perform(get(NOTIFICACIONES + "/" + id))
                .andExpect(jsonPath("$.fechaLectura").value(fechaLectura));
    }

    @Test
    void api12Devuelve404Y422() throws Exception {
        Long email = crearNotificacion(
                "{\"destinatarioId\":10,\"plantillaId\":1,\"parametros\":{\"nombre\":\"Ana\",\"pedidoId\":\"5\"}}");

        mockMvc.perform(patch(NOTIFICACIONES + "/99999/read"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
        mockMvc.perform(patch(NOTIFICACIONES + "/" + email + "/read"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_IN_APP"));
    }

    @Test
    void api04FiltraLasNoLeidasDeLaBandejaInterna() throws Exception {
        Long sinLeer = crearInApp(11);
        Long leida = crearInApp(11);
        mockMvc.perform(patch(NOTIFICACIONES + "/" + leida + "/read")).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/11/notifications").param("canal", "IN_APP").param("leida", "false")
                .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(sinLeer.intValue())))
                .andExpect(jsonPath("$.content[?(@.id == " + leida + ")]").isEmpty());
    }
}
