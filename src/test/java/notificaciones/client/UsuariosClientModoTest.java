package notificaciones.client;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

// notificaciones.usuarios.modo elige la implementación de UsuariosClient (INT-01)
class UsuariosClientModoTest {

    @Nested
    @SpringBootTest
    class PorDefecto {
        @Autowired
        private UsuariosClient usuariosClient;

        @Test
        void sinConfigurarSeUsaElSimulado() {
            assertInstanceOf(UsuariosClientSimulado.class, usuariosClient);
        }
    }

    @Nested
    @SpringBootTest
    @TestPropertySource(properties = "notificaciones.usuarios.modo=simulado")
    class ModoSimulado {
        @Autowired
        private UsuariosClient usuariosClient;

        @Test
        void conModoSimuladoSeUsaElSimulado() {
            assertInstanceOf(UsuariosClientSimulado.class, usuariosClient);
        }
    }

    @Nested
    @SpringBootTest
    @TestPropertySource(properties = { "notificaciones.usuarios.modo=http",
            "notificaciones.usuarios.base-url=http://127.0.0.1:1" })
    class ModoHttp {
        @Autowired
        private UsuariosClient usuariosClient;

        @Test
        void conModoHttpSeUsaElClienteHttp() {
            assertInstanceOf(UsuariosClientHttp.class, usuariosClient);
        }
    }
}
