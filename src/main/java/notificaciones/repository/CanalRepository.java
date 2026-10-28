package notificaciones.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import notificaciones.model.Canal;
import notificaciones.model.TipoCanal;

public interface CanalRepository extends JpaRepository<Canal, Long> {
    // REP-06
    Optional<Canal> findByTipo(TipoCanal tipo);
}
