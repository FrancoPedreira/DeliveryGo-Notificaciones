package notificaciones.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import notificaciones.dto.CanalDto;
import notificaciones.dto.PlantillaDto;
import notificaciones.services.CanalServices;
import notificaciones.services.PlantillaServices;

// Datos iniciales: la administración de plantillas (CU-07) queda para la entrega final
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
        CanalDto email = canalServices.crearCanal(new CanalDto("EMAIL", "Correo electrónico", true));
        CanalDto push = canalServices.crearCanal(new CanalDto("PUSH", "Notificación push", true));
        CanalDto inApp = canalServices.crearCanal(new CanalDto("IN_APP", "Bandeja interna", true));

        plantillaServices.crearPlantilla(new PlantillaDto("PEDIDO_CONFIRMADO_EMAIL", "Pedido confirmado",
                "Hola {nombre}, tu pedido {pedidoId} fue confirmado.", true, email));
        plantillaServices.crearPlantilla(new PlantillaDto("PAGO_APROBADO_PUSH", "Pago aprobado",
                "Tu pago de {monto} fue aprobado.", true, push));
        plantillaServices.crearPlantilla(new PlantillaDto("ENVIO_EN_CAMINO_IN_APP", "Envío en camino",
                "Tu envío {codigo} está en camino.", true, inApp));
    }
}
