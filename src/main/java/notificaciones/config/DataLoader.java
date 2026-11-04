package notificaciones.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import notificaciones.dto.PlantillaRequestDto;
import notificaciones.model.Canal;
import notificaciones.model.TipoCanal;
import notificaciones.repository.CanalRepository;
import notificaciones.services.PlantillaServices;

// Datos iniciales; después las plantillas se administran por API (CU-07) y los canales se habilitan o
// deshabilitan por API (CU-08). Los canales no tienen alta por API: son siempre estos tres
@Component
public class DataLoader implements CommandLineRunner {
    private final CanalRepository canalRepository;
    private final PlantillaServices plantillaServices;

    public DataLoader(CanalRepository canalRepository, PlantillaServices plantillaServices) {
        this.canalRepository = canalRepository;
        this.plantillaServices = plantillaServices;
    }

    @Override
    public void run(String... args) {
        canalRepository.save(new Canal(TipoCanal.EMAIL, "Correo electrónico", true));
        canalRepository.save(new Canal(TipoCanal.PUSH, "Notificación push", true));
        canalRepository.save(new Canal(TipoCanal.IN_APP, "Bandeja interna", true));

        plantillaServices.crearPlantilla(new PlantillaRequestDto("PEDIDO_CONFIRMADO_EMAIL", "Pedido confirmado",
                "Hola {nombre}, tu pedido {pedidoId} fue confirmado.", true, "EMAIL"));
        plantillaServices.crearPlantilla(new PlantillaRequestDto("PAGO_APROBADO_PUSH", "Pago aprobado",
                "Tu pago de {monto} fue aprobado.", true, "PUSH"));
        plantillaServices.crearPlantilla(new PlantillaRequestDto("ENVIO_EN_CAMINO_IN_APP", "Envío en camino",
                "Tu envío {codigo} está en camino.", true, "IN_APP"));
    }
}
