package com.migal.smartops.abastecimiento.repository;

import com.migal.smartops.abastecimiento.model.OrdenCompra;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a las órdenes de compra. Las operaciones que cambian la orden la bloquean antes de modificarla (ADR-010), de
 * modo que una recepción validada y una modificación concurrente no se intercalen.
 */
public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long>, JpaSpecificationExecutor<OrdenCompra> {

	/** Indica si el proveedor tiene órdenes registradas, sin importar su estado. */
	boolean existsByProveedorId(Long idProveedor);

	/** Bloquea la orden hasta el fin de la transacción. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from OrdenCompra o where o.id = :id")
	Optional<OrdenCompra> bloquearPorId(@Param("id") Long id);
}
