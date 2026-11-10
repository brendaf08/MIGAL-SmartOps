package com.migal.smartops.comercial.repository;

import com.migal.smartops.comercial.model.Venta;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a datos de las ventas. Las transiciones de estado bloquean la fila antes de modificarla (ADR-010). */
public interface VentaRepository extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

	/** Bloquea la venta hasta el fin de la transacción para que dos transiciones no se apliquen en paralelo. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select v from Venta v where v.id = :id")
	Optional<Venta> bloquearPorId(@Param("id") Long id);

	/** Indica si la cotización ya originó una venta. */
	boolean existsByCotizacionId(Long idCotizacion);
}
