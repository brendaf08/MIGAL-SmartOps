package com.migal.smartops.inventario.repository;

import com.migal.smartops.inventario.model.Inventario;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a las existencias por producto y ubicación. Las lecturas destinadas a modificar el saldo se bloquean
 * en orden ascendente de identificador (ADR-010) para impedir interbloqueos entre transacciones concurrentes.
 */
public interface InventarioRepository extends JpaRepository<Inventario, Long> {

	Optional<Inventario> findByProductoIdAndAlmacenId(Long idProducto, Long idAlmacen);

	List<Inventario> findByProductoId(Long idProducto);

	/**
	 * Crea la existencia en cero si aún no existe. Es seguro ante solicitudes concurrentes: si otra transacción
	 * ya la creó, no hace nada.
	 */
	@Modifying
	@Query(value = "INSERT INTO inventario (id_producto, id_almacen, cantidad_fisica) VALUES (:idProducto, :idAlmacen, 0) "
			+ "ON CONFLICT (id_producto, id_almacen) DO NOTHING", nativeQuery = true)
	int crearSiNoExiste(@Param("idProducto") Long idProducto, @Param("idAlmacen") Long idAlmacen);

	/** Bloquea las filas indicadas, en orden ascendente de identificador, hasta el fin de la transacción. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from Inventario i where i.id in :ids order by i.id")
	List<Inventario> bloquearPorIds(@Param("ids") Collection<Long> ids);

	/** Bloquea las filas de un producto en las ubicaciones indicadas, en orden ascendente de identificador. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from Inventario i where i.producto.id = :idProducto and i.almacen.id in :idsAlmacen order by i.id")
	List<Inventario> bloquearPorProductoYAlmacenes(@Param("idProducto") Long idProducto,
			@Param("idsAlmacen") Collection<Long> idsAlmacen);
}
