package notificaciones.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import notificaciones.model.Notificacion;

// REP-01, REP-03 y REP-04. REP-08 (CU-06): findAll(Specification, Pageable) para el historial filtrado y paginado
public interface NotificacionRepository
        extends JpaRepository<Notificacion, Long>, JpaSpecificationExecutor<Notificacion> {

    // REP-07 (DM-09): bloqueo pesimista de escritura (SELECT ... FOR UPDATE) para que dos reintentos
    // simultáneos de la misma notificación se procesen de a uno
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notificacion n where n.id = :id")
    Optional<Notificacion> findByIdParaActualizar(@Param("id") Long id);
}
