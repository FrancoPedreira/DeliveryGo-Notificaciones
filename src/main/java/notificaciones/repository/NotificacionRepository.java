package notificaciones.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import notificaciones.model.EstadoNotificacion;
import notificaciones.model.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByEstado(EstadoNotificacion estado);

}
