package com.migal.smartops.catalogo.repository;

import com.migal.smartops.catalogo.model.Producto;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Acceso a datos del catálogo maestro de productos. Los filtros combinados del listado se componen con
 * especificaciones en la capa de servicio; los métodos de esta interfaz cubren las búsquedas directas.
 */
public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

	/** Búsqueda por clave natural. */
	Optional<Producto> findBySku(String sku);

	boolean existsBySku(String sku);

	/** Búsqueda de texto libre por SKU o por nombre oficial, sin distinguir mayúsculas. */
	Page<Producto> findBySkuContainingIgnoreCaseOrNombreOficialContainingIgnoreCase(String sku, String nombre,
			Pageable paginacion);

	/** Productos de una categoría con la vigencia indicada. */
	Page<Producto> findByCategoriaIdAndEstadoActivo(Long idCategoria, boolean estadoActivo, Pageable paginacion);
}
