package com.migal.smartops.catalogo.controller;

import com.migal.smartops.catalogo.controller.SolicitudesCatalogo.ActualizacionEstado;
import com.migal.smartops.catalogo.controller.SolicitudesCatalogo.ProductoActualizacion;
import com.migal.smartops.catalogo.controller.SolicitudesCatalogo.ProductoRegistro;
import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.service.ProductoService;
import com.migal.smartops.comun.web.Ordenamiento;
import com.migal.smartops.comun.web.PaginaDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogo maestro de productos (CU-03). Lo utilizan el Almacenero y el Administrador. Las operaciones
 * declaran sus roles en el contrato mediante {@code @PreAuthorize}.
 */
@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {

	private static final String ROLES_GESTION = "hasAnyRole('ALMACENERO','ADMINISTRADOR')";

	private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
			"sku", "sku",
			"nombreOficial", "nombreOficial",
			"fechaRegistro", "fechaRegistro");

	private final ProductoService productos;

	public ProductoController(ProductoService productos) {
		this.productos = productos;
	}

	@GetMapping
	@PreAuthorize(ROLES_GESTION)
	public PaginaDto<ProductoDto> listar(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String sku,
			@RequestParam(required = false) Long idCategoria,
			@RequestParam(required = false) Boolean estadoActivo,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
			@RequestParam(required = false) String orden) {
		Pageable paginacion = Ordenamiento.paginacion(page, pageSize, orden, CAMPOS_ORDENABLES, Sort.by("sku"));
		return PaginaDto.de(productos.listar(q, sku, idCategoria, estadoActivo, paginacion), ProductoDto::de);
	}

	@PostMapping
	@PreAuthorize(ROLES_GESTION)
	public ResponseEntity<ProductoDto> registrar(@Valid @RequestBody ProductoRegistro solicitud) {
		Producto creado = productos.registrar(solicitud.sku(), solicitud.datos(),
				Objects.requireNonNullElse(solicitud.estadoActivo(), true));
		return ResponseEntity.created(URI.create("/api/v1/productos/" + creado.getId())).body(ProductoDto.de(creado));
	}

	@GetMapping("/{id}")
	@PreAuthorize(ROLES_GESTION)
	public ProductoDto consultar(@PathVariable String id) {
		return ProductoDto.de(productos.consultar(Identificadores.numero(id)));
	}

	@PutMapping("/{id}")
	@PreAuthorize(ROLES_GESTION)
	public ProductoDto actualizar(@PathVariable String id, @Valid @RequestBody ProductoActualizacion solicitud) {
		return ProductoDto.de(productos.actualizar(Identificadores.numero(id), solicitud.datos()));
	}

	@PutMapping("/{id}/estado")
	@PreAuthorize(ROLES_GESTION)
	public ProductoDto cambiarEstado(@PathVariable String id, @Valid @RequestBody ActualizacionEstado solicitud) {
		return ProductoDto.de(productos.cambiarEstado(Identificadores.numero(id), solicitud.estadoActivo()));
	}
}
