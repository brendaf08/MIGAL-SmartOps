package com.migal.smartops.catalogo.repository;

import com.migal.smartops.catalogo.model.Categoria;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de las categorías de producto. Las categorías son datos de referencia de solo lectura en la versión 1. */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

	/** Búsqueda por denominación. Los nombres no se repiten en el catálogo sembrado. */
	Optional<Categoria> findByNombre(String nombre);
}
