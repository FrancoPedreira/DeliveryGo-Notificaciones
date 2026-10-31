package notificaciones.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import notificaciones.model.IntentoEnvio;

// REP-05
public interface IntentoEnvioRepository extends JpaRepository<IntentoEnvio, Long> {

    // REP-12 (CU-09): los intentos de una notificación en el orden en que ocurrieron
    List<IntentoEnvio> findByNotificacionIdOrderByNumeroIntentoAsc(Long notificacionId);
}
