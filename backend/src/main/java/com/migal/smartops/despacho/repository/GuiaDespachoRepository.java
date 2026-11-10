package com.migal.smartops.despacho.repository;

import com.migal.smartops.despacho.model.GuiaDespacho;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a las guías de despacho. Las transiciones de estado bloquean la fila antes de modificarla (ADR-010), en el
 * mismo orden que las ventas: primero la venta y después la guía.
 */
public interface GuiaDespachoRepository extends JpaRepository<GuiaDespacho, Long>, JpaSpecificationExecutor<GuiaDespacho> {

	/** Indica si la venta ya origina una guía, sin importar el estado de esta. */
	boolean existsByVentaId(Long idVenta);

	/** Identificador de la venta de origen de una guía, sin bloquear la fila. */
	@Query("select g.venta.id from GuiaDespacho g where g.id = :id")
	Optional<Long> idVentaDeGuia(@Param("id") Long id);

	/** Bloquea la guía hasta el fin de la transacción. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select g from GuiaDespacho g where g.id = :id")
	Optional<GuiaDespacho> bloquearPorId(@Param("id") Long id);

	/** Bloquea la guía de una venta hasta el fin de la transacción. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select g from GuiaDespacho g where g.venta.id = :idVenta")
	Optional<GuiaDespacho> bloquearPorVenta(@Param("idVenta") Long idVenta);
}
