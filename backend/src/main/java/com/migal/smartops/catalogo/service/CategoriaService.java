package com.migal.smartops.catalogo.service;

import com.migal.smartops.catalogo.model.Categoria;
import com.migal.smartops.catalogo.repository.CategoriaRepository;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta de categorías de producto (solo lectura en la versión 1). */
@Service
public class CategoriaService {

	private final CategoriaRepository categorias;

	public CategoriaService(CategoriaRepository categorias) {
		this.categorias = categorias;
	}

	/** Lista completa de categorías, en el orden de su identificador. Es un catálogo acotado y no se pagina. */
	@Transactional(readOnly = true)
	public List<Categoria> listar() {
		return categorias.findAll(Sort.by("id"));
	}

	/**
	 * Verifica que la categoría exista y la devuelve.
	 *
	 * @throws ErrorDeNegocio con {@code CATEGORIA_NO_ENCONTRADA} si el identificador no corresponde a una categoría
	 */
	@Transactional(readOnly = true)
	public Categoria validarExistencia(long id) {
		return categorias.findById(id).orElseThrow(() -> new ErrorDeNegocio(CodigoError.CATEGORIA_NO_ENCONTRADA,
				"La categoría indicada no existe."));
	}
}
