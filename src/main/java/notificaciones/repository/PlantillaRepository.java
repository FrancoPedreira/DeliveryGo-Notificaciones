package notificaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import notificaciones.model.Plantilla;

// REP-02. REP-09 y REP-10 (CU-07): save / existencia por nombre y listado
public interface PlantillaRepository extends JpaRepository<Plantilla, Long> {

    // REP-09: el nombre de una plantilla es único (RN-10)
    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Long id);
}
