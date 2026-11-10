package com.migal.smartops.abastecimiento.repository;

import com.migal.smartops.abastecimiento.model.CatalogoProveedor;
import com.migal.smartops.abastecimiento.model.CatalogoProveedorId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso al catálogo de productos de cada proveedor. */
public interface CatalogoProveedorRepository extends JpaRepository<CatalogoProveedor, CatalogoProveedorId> {

	/** Productos del catálogo de un proveedor, en orden ascendente de producto. */
	List<CatalogoProveedor> findByIdIdProveedorOrderByIdIdProductoAsc(Long idProveedor);

	/** Identificadores de los proveedores que suministran el producto indicado. */
	@Query("select c.id.idProveedor from CatalogoProveedor c where c.producto.sku = :sku")
	List<Long> idsProveedoresPorSku(@Param("sku") String sku);
}
