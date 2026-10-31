package notificaciones.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import notificaciones.model.Canal;
import notificaciones.model.TipoCanal;

public interface CanalRepository extends JpaRepository<Canal, Long> {
    // REP-06. REP-11 (CU-08): findAll() / save(...) para listar y habilitar o deshabilitar canales
    Optional<Canal> findByTipo(TipoCanal tipo);
}
