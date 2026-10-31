package notificaciones.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import notificaciones.dto.CanalDto;
import notificaciones.dto.PlantillaRequestDto;
import notificaciones.services.CanalServices;
import notificaciones.services.PlantillaServices;

// Datos iniciales; después las plantillas se administran por API (CU-07) y los canales por API (CU-08)
@Component
public class DataLoader implements CommandLineRunner {
    private final CanalServices canalServices;
    private final PlantillaServices plantillaServices;

    public DataLoader(CanalServices canalServices, PlantillaServices plantillaServices) {
        this.canalServices = canalServices;
        this.plantillaServices = plantillaServices;
    }

    @Override
    public void run(String... args) {
        canalServices.crearCanal(new CanalDto("EMAIL", "Correo electrónico", true));
        canalServices.crearCanal(new CanalDto("PUSH", "Notificación push", true));
        canalServices.crearCanal(new CanalDto("IN_APP", "Bandeja interna", true));

        plantillaServices.crearPlantilla(new PlantillaRequestDto("PEDIDO_CONFIRMADO_EMAIL", "Pedido confirmado",
                "Hola {nombre}, tu pedido {pedidoId} fue confirmado.", true, "EMAIL"));
        plantillaServices.crearPlantilla(new PlantillaRequestDto("PAGO_APROBADO_PUSH", "Pago aprobado",
                "Tu pago de {monto} fue aprobado.", true, "PUSH"));
        plantillaServices.crearPlantilla(new PlantillaRequestDto("ENVIO_EN_CAMINO_IN_APP", "Envío en camino",
                "Tu envío {codigo} está en camino.", true, "IN_APP"));
    }
}
