package notificaciones.security;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import jakarta.servlet.Filter;

// JWT (401 / 403 por rol) y CORS. Arma su propio MockMvc para no heredar el token ADMIN por defecto
@SpringBootTest
class SeguridadTest {
    private static final String CANALES = "/api/v1/channels";
    private static final String PORTAL = "http://localhost:5173";

    @Autowired
    private WebApplicationContext contexto;
    @Autowired
    private TokenDePrueba tokens;
    @Value("${notificaciones.jwt.secret}")
    private String secret;

    private MockMvc mockMvc;

    @BeforeEach
    void armarMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto)
                .addFilters(contexto.getBean("correlationIdFilter", Filter.class),
                        contexto.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void sinTokenDevuelve401ConElFormatoDeError() throws Exception {
        mockMvc.perform(get(CANALES))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value(CANALES))
                .andExpect(jsonPath("$.correlationId").value(notNullValue()));
    }

    @Test
    void tokenConFirmaInvalidaDevuelve401() throws Exception {
        String firmadoConOtraClave = TokenDePrueba.crear(
                "otra-clave-distinta-de-al-menos-32-caracteres",
                10L,
                "ADMIN",
                new Date(System.currentTimeMillis() + 60_000));

        mockMvc.perform(get(CANALES).header("Authorization", bearer(firmadoConOtraClave)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenVencidoDevuelve401() throws Exception {
        String vencido = TokenDePrueba.crear(
                secret,
                10L,
                "ADMIN",
                new Date(System.currentTimeMillis() - 60_000));

        mockMvc.perform(get(CANALES).header("Authorization", bearer(vencido)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenMalFormadoDevuelve401() throws Exception {
        mockMvc.perform(get(CANALES).header("Authorization", "Bearer esto-no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenValidoDeCualquierRolPuedeConsultar() throws Exception {
        mockMvc.perform(get(CANALES).header("Authorization", bearer(tokens.crear(10L, "CLIENTE"))))
                .andExpect(status().isOk());
    }

    @Test
    void administrarCanalesSinRolAdminDevuelve403() throws Exception {
        mockMvc.perform(patch(CANALES + "/PUSH")
                .header("Authorization", bearer(tokens.crear(10L, "CLIENTE")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void administrarCanalesConRolAdminFunciona() throws Exception {
        mockMvc.perform(patch(CANALES + "/PUSH")
                .header("Authorization", bearer(tokens.crear(1L, "ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerEsPublico() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk());
    }

    @Test
    void statusEsPublicoYDevuelve200() throws Exception {
        mockMvc.perform(get("/status")).andExpect(status().isOk());
    }

    // CORS: el navegador pregunta con OPTIONS (preflight, sin token) antes de la llamada real
    @Test
    void preflightDesdeElPortalEstaPermitido() throws Exception {
        mockMvc.perform(options(CANALES).header("Origin", PORTAL)
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", PORTAL));
    }

    @Test
    void preflightDesdeOtroOrigenSeRechaza() throws Exception {
        mockMvc.perform(options(CANALES).header("Origin", "http://sitio-desconocido.com")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
