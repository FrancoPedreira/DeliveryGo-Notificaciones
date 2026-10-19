package notificaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import notificaciones.model.Canal;
import notificaciones.model.TipoCanal;

public interface CanalRepository extends JpaRepository<Canal, Long> {
    Canal findByTipo(TipoCanal tipo);

}
