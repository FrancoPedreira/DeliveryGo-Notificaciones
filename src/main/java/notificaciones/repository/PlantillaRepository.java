package notificaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import notificaciones.model.Plantilla;

public interface PlantillaRepository extends JpaRepository<Plantilla, Long> {
    Plantilla findByNombre(String nombre);

}
