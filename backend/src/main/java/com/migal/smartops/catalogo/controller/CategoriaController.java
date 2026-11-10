package com.migal.smartops.catalogo.controller;

import com.migal.smartops.catalogo.service.CategoriaService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consulta de categorías de producto, de solo lectura en la versión 1. */
@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaController {

	private final CategoriaService categorias;

	public CategoriaController(CategoriaService categorias) {
		this.categorias = categorias;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ALMACENERO','ADMINISTRADOR')")
	public List<CategoriaDto> listar() {
		return categorias.listar().stream().map(CategoriaDto::de).toList();
	}
}
