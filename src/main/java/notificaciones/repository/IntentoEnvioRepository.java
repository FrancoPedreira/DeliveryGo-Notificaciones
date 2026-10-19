package notificaciones.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import notificaciones.model.IntentoEnvio;

public interface IntentoEnvioRepository extends JpaRepository<IntentoEnvio, Long> {
    List<IntentoEnvio> findByNotificacionIdOrderByNumeroIntento(Long notificacionId);

}
