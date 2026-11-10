package com.migal.smartops.inventario.repository;

import com.migal.smartops.inventario.model.ReservaStock;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a las reservas de stock. */
public interface ReservaStockRepository extends JpaRepository<ReservaStock, Long> {

	/** Reservas activas de una línea de venta. */
	@Query("select r from ReservaStock r where r.idDetalleVenta = :idDetalle and r.estado = 'activa' order by r.id")
	List<ReservaStock> findActivasPorDetalleVenta(@Param("idDetalle") Long idDetalleVenta);
}
