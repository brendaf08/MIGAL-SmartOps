package com.migal.smartops.inventario.repository;

import com.migal.smartops.inventario.model.MovimientoKardex;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Acceso al Kardex. Solo se anexan movimientos; la base de datos rechaza modificaciones y eliminaciones. */
public interface MovimientoKardexRepository
		extends JpaRepository<MovimientoKardex, Long>, JpaSpecificationExecutor<MovimientoKardex> {

	/** Indica si un movimiento ya fue compensado por otro. */
	boolean existsByIdMovimientoCorregido(Long idMovimiento);

	/** Movimientos de un traslado, en orden de registro (salida y entrada). */
	List<MovimientoKardex> findByReferenciaTrasladoOrderByIdAsc(UUID referencia);
}
