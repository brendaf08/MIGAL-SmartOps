package com.migal.smartops.catalogo.service;

import com.migal.smartops.catalogo.model.Almacen;
import com.migal.smartops.catalogo.repository.AlmacenRepository;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta y activación de las ubicaciones de inventario (tienda principal y almacenes). */
@Service
public class AlmacenService {

	private final AlmacenRepository almacenes;

	public AlmacenService(AlmacenRepository almacenes) {
		this.almacenes = almacenes;
	}

	/** Lista completa de ubicaciones, en el orden de su identificador. Es un catálogo acotado y no se pagina. */
	@Transactional(readOnly = true)
	public List<Almacen> listar() {
		return almacenes.findAll(Sort.by("id"));
	}

	/** Activa o desactiva la disponibilidad de la ubicación. La desactivación es lógica y conserva el registro. */
	@Transactional
	public Almacen cambiarEstado(long id, boolean activo) {
		Almacen almacen = almacenes.findById(id).orElseThrow(() -> new ErrorDeNegocio(
				CodigoError.RECURSO_NO_ENCONTRADO, "El almacén no existe."));
		almacen.cambiarEstado(activo);
		return almacenes.save(almacen);
	}
}
