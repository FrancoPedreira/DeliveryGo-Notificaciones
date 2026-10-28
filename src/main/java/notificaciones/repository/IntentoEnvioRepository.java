package notificaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import notificaciones.model.IntentoEnvio;

// REP-05
public interface IntentoEnvioRepository extends JpaRepository<IntentoEnvio, Long> {
}
