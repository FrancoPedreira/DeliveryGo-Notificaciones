package notificaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import notificaciones.model.Notificacion;

// REP-01, REP-03 y REP-04
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
}
