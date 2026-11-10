package com.migal.smartops.catalogo.repository;

import com.migal.smartops.catalogo.model.Almacen;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de las ubicaciones de inventario (tienda principal y almacenes). */
public interface AlmacenRepository extends JpaRepository<Almacen, Long> {
}
