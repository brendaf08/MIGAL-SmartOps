package com.migal.smartops.abastecimiento.repository;

import com.migal.smartops.abastecimiento.model.OrdenCompra;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a las órdenes de compra. Las operaciones que cambian la orden la bloquean antes de modificarla (ADR-010), de
 * modo que una recepción validada y una modificación concurrente no se intercalen. Las consultas de cantidades
 * recibidas leen las recepciones validadas de la orden, cuya tabla pertenece al módulo de recepciones.
 */
public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long>, JpaSpecificationExecutor<OrdenCompra> {

	/** Bloquea la orden hasta el fin de la transacción. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from OrdenCompra o where o.id = :id")
	Optional<OrdenCompra> bloquearPorId(@Param("id") Long id);

	/** Cantidad recibida por producto en las recepciones validadas de la orden: pares de producto y suma. */
	@Query("select d.id.idProducto, sum(d.cantidadRecibida) from DetalleRecepcion d "
			+ "where d.recepcion.orden.id = :idOrden and d.recepcion.estado = 'validada' group by d.id.idProducto")
	List<Object[]> recibidoPorProducto(@Param("idOrden") Long idOrden);

	/** Cantidad de recepciones validadas de la orden. */
	@Query("select count(r) from Recepcion r where r.orden.id = :idOrden and r.estado = 'validada'")
	long contarRecepcionesValidadas(@Param("idOrden") Long idOrden);

	/** Cantidad de recepciones de la orden en cualquier estado. */
	@Query("select count(r) from Recepcion r where r.orden.id = :idOrden")
	long contarRecepciones(@Param("idOrden") Long idOrden);
}
