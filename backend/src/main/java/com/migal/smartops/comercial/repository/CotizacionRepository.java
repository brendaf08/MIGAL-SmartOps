package com.migal.smartops.comercial.repository;

import com.migal.smartops.comercial.model.Cotizacion;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a datos de las cotizaciones. Las transiciones de estado bloquean la fila antes de modificarla (ADR-010). */
public interface CotizacionRepository extends JpaRepository<Cotizacion, Long>, JpaSpecificationExecutor<Cotizacion> {

	/** Bloquea la cotización hasta el fin de la transacción para que dos transiciones no se apliquen en paralelo. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Cotizacion c where c.id = :id")
	Optional<Cotizacion> bloquearPorId(@Param("id") Long id);

	/**
	 * Pasa a vencida las cotizaciones emitidas, aceptadas o validadas cuya fecha de vencimiento es anterior a la fecha
	 * indicada. Las cotizaciones que ya originaron una venta no se modifican.
	 *
	 * @return cantidad de cotizaciones vencidas por esta operación
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE cotizacion SET estado = 'vencida'
			 WHERE estado IN ('emitida', 'aceptada', 'validada')
			   AND fecha_vencimiento < :hoy
			   AND NOT EXISTS (SELECT 1 FROM venta v WHERE v.id_cotizacion = cotizacion.id_cotizacion)
			""", nativeQuery = true)
	int vencerVencidas(@Param("hoy") LocalDate hoy);

	/** Indica si la cotización ya originó una venta. La tabla venta pertenece al módulo de ventas. */
	@Query(value = "SELECT EXISTS (SELECT 1 FROM venta v WHERE v.id_cotizacion = :id)", nativeQuery = true)
	boolean existeVenta(@Param("id") Long id);
}
