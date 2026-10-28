package notificaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import notificaciones.model.Plantilla;

// REP-02
public interface PlantillaRepository extends JpaRepository<Plantilla, Long> {
}
