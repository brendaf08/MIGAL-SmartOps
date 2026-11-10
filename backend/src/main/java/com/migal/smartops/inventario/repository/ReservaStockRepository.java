package com.migal.smartops.inventario.repository;

import com.migal.smartops.inventario.model.ReservaStock;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a las reservas de stock. */
public interface ReservaStockRepository extends JpaRepository<ReservaStock, Long> {

	/** Suma de las reservas activas de un inventario. */
	@Query("select coalesce(sum(r.cantidadReservada), 0) from ReservaStock r where r.inventario.id = :idInventario and r.estado = 'activa'")
	BigDecimal sumaActivas(@Param("idInventario") Long idInventario);

	/** Reservas activas de varias líneas de venta. */
	@Query("select r from ReservaStock r where r.idDetalleVenta in :ids and r.estado = 'activa' order by r.id")
	List<ReservaStock> findActivasPorDetallesVenta(@Param("ids") java.util.Collection<Long> idsDetalleVenta);

	/** Reservas activas de una línea de venta. */
	@Query("select r from ReservaStock r where r.idDetalleVenta = :idDetalle and r.estado = 'activa' order by r.id")
	List<ReservaStock> findActivasPorDetalleVenta(@Param("idDetalle") Long idDetalleVenta);
}
