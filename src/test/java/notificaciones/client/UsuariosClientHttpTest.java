package notificaciones.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpServer;

import notificaciones.dto.UsuarioContactoResponseDto;
import notificaciones.exception.ServicioNoDisponibleException;

// INT-01 real: mapeo de respuestas del cliente HTTP hacia Usuarios (200, 404, 5xx, timeout)
class UsuariosClientHttpTest {
    private static final String BASE_URL = "http://usuarios.test";
    private static final String API_KEY = "api-key-de-prueba-notificaciones-123";
    private static final String URL_USUARIO_10 = BASE_URL + "/api/v1/users/10";
    private static final String CONTACTO_JSON = """
            {
              "id": 10,
              "nombre": "Ana",
              "apellido": "Pérez",
              "email": "ana@mail.com",
              "telefono": null,
              "rol": "CLIENTE",
              "estado": "ACTIVO",
              "fechaAlta": "2026-10-30T15:00:00Z"
            }""";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
    private final UsuariosClientHttp cliente =
            new UsuariosClientHttp(builder.baseUrl(BASE_URL).build(), API_KEY);

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @Test
    void respuesta200DevuelveElPerfilYMapeaElEstado() {
        servidor.expect(requestTo(URL_USUARIO_10))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Api-Key", API_KEY))
                .andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess(CONTACTO_JSON, MediaType.APPLICATION_JSON));

        Optional<UsuarioContactoResponseDto> contacto = cliente.obtenerContacto(10L);

        assertTrue(contacto.isPresent());
        assertEquals(10L, contacto.get().getId());
        assertEquals("Ana", contacto.get().getNombre());
        assertEquals("ana@mail.com", contacto.get().getEmail());
        assertTrue(contacto.get().isActivo());
        assertNull(contacto.get().getPushToken());
        servidor.verify();
    }

    @Test
    void respuesta404DevuelveVacio() {
        servidor.expect(requestTo(BASE_URL + "/api/v1/users/99"))
                .andExpect(header("X-Api-Key", API_KEY))
                .andRespond(withResourceNotFound());

        assertFalse(cliente.obtenerContacto(99L).isPresent());
        servidor.verify();
    }

    @Test
    void respuesta500LanzaServicioNoDisponible() {
        servidor.expect(requestTo(URL_USUARIO_10)).andRespond(withServerError());

        ServicioNoDisponibleException e = assertThrows(ServicioNoDisponibleException.class,
                () -> cliente.obtenerContacto(10L));
        assertEquals("USERS_SERVICE_UNAVAILABLE", e.getCode());
    }

    @Test
    void otroEstadoInesperadoTambienLanzaServicioNoDisponible() {
        servidor.expect(requestTo(URL_USUARIO_10))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.obtenerContacto(10L));
    }

    @Test
    void errorDeLecturaLanzaServicioNoDisponible() {
        servidor.expect(requestTo(URL_USUARIO_10)).andRespond(request -> {
            throw new SocketTimeoutException("read timed out");
        });

        ServicioNoDisponibleException e = assertThrows(ServicioNoDisponibleException.class,
                () -> cliente.obtenerContacto(10L));
        assertEquals("USERS_SERVICE_UNAVAILABLE", e.getCode());
    }

    @Test
    void cuerpoIlegibleLanzaServicioNoDisponible() {
        servidor.expect(requestTo(URL_USUARIO_10))
                .andRespond(withSuccess("esto no es json", MediaType.APPLICATION_JSON));

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.obtenerContacto(10L));
    }

    @Test
    void propagaElCorrelationIdDeLaSolicitudEntrante() {
        MDC.put("correlationId", "pedido-123");
        servidor.expect(requestTo(URL_USUARIO_10))
                .andExpect(header("X-Correlation-Id", "pedido-123"))
                .andExpect(header("X-Api-Key", API_KEY))
                .andRespond(withSuccess(CONTACTO_JSON, MediaType.APPLICATION_JSON));

        cliente.obtenerContacto(10L);
        servidor.verify();
    }

    @Test
    void sinCorrelationIdNoEnviaElHeader() {
        servidor.expect(requestTo(URL_USUARIO_10))
                .andExpect(headerDoesNotExist("X-Correlation-Id"))
                .andExpect(header("X-Api-Key", API_KEY))
                .andRespond(withSuccess(CONTACTO_JSON, MediaType.APPLICATION_JSON));

        cliente.obtenerContacto(10L);
        servidor.verify();
    }

    // Servidor real que tarda más que el timeout: comprueba que el timeout configurado corta la espera
    @Test
    void siUsuariosTardaMasQueElTimeoutLanzaServicioNoDisponible() throws IOException {
        HttpServer lento = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        lento.createContext("/", exchange -> {
            try {
                Thread.sleep(1500);
                exchange.sendResponseHeaders(200, -1);
            } catch (InterruptedException | IOException ignorada) {
                // el cliente ya cortó la conexión
            } finally {
                exchange.close();
            }
        });
        lento.start();
        try {
            UsuariosClientHttp conTimeoutCorto = new UsuariosClientHttp(
                    "http://127.0.0.1:" + lento.getAddress().getPort(), 200, API_KEY);

            long inicio = System.nanoTime();
            assertThrows(ServicioNoDisponibleException.class, () -> conTimeoutCorto.obtenerContacto(10L));
            long ms = (System.nanoTime() - inicio) / 1_000_000;

            assertTrue(ms < 1400, "Debería cortar cerca del timeout de 200 ms y tardó " + ms + " ms");
        } finally {
            lento.stop(0);
        }
    }

    @Test
    void conexionRechazadaLanzaServicioNoDisponible() {
        // Puerto 1: nadie escucha
        UsuariosClientHttp sinServidor = new UsuariosClientHttp("http://127.0.0.1:1", 500, API_KEY);

        assertThrows(ServicioNoDisponibleException.class, () -> sinServidor.obtenerContacto(10L));
    }
}
