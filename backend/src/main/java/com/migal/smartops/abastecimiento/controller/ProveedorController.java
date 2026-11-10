package com.migal.smartops.abastecimiento.controller;

import com.migal.smartops.abastecimiento.controller.AbastecimientoDtos.CatalogoItemDto;
import com.migal.smartops.abastecimiento.controller.AbastecimientoDtos.ProveedorDto;
import com.migal.smartops.abastecimiento.controller.SolicitudesAbastecimiento.ActualizacionEstado;
import com.migal.smartops.abastecimiento.controller.SolicitudesAbastecimiento.CatalogoActualizacion;
import com.migal.smartops.abastecimiento.controller.SolicitudesAbastecimiento.CatalogoEntrada;
import com.migal.smartops.abastecimiento.controller.SolicitudesAbastecimiento.ProveedorActualizacion;
import com.migal.smartops.abastecimiento.controller.SolicitudesAbastecimiento.ProveedorRegistro;
import com.migal.smartops.abastecimiento.service.ProveedorService;
import com.migal.smartops.abastecimiento.service.ProveedorService.DatosProveedor;
import com.migal.smartops.abastecimiento.service.ProveedorService.EntradaCatalogo;
import com.migal.smartops.catalogo.controller.Identificadores;
import com.migal.smartops.comun.web.Ordenamiento;
import com.migal.smartops.comun.web.PaginaDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
 * Directorio de proveedores y su catálogo de productos (CU-11). Las operaciones las ejecutan el Reponedor y el
 * Administrador, según el contrato. La vigencia y el catálogo se modifican con sus operaciones propias.
 */
@RestController
@RequestMapping("/api/v1/proveedores")
public class ProveedorController {

	private static final String ROLES = "hasAnyRole('REPONEDOR','ADMINISTRADOR')";
	private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
			"razonSocial", "razonSocial", "ruc", "ruc", "fechaRegistro", "fechaRegistro");

	private final ProveedorService proveedores;

	public ProveedorController(ProveedorService proveedores) {
		this.proveedores = proveedores;
	}

	@GetMapping
	@PreAuthorize(ROLES)
	public PaginaDto<ProveedorDto> listar(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) Boolean estadoActivo,
			@RequestParam(required = false) String sku,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
			@RequestParam(required = false) String orden) {
		var paginacion = Ordenamiento.paginacion(page, pageSize, orden, CAMPOS_ORDENABLES,
				Sort.by(Sort.Direction.ASC, "razonSocial"));
		return PaginaDto.de(proveedores.listar(q, estadoActivo, sku, paginacion), ProveedorDto::de);
	}

	@PostMapping
	@PreAuthorize(ROLES)
	public ResponseEntity<ProveedorDto> registrar(@Valid @RequestBody ProveedorRegistro solicitud) {
		var proveedor = proveedores.registrar(new DatosProveedor(solicitud.ruc(), solicitud.razonSocial(),
				solicitud.contacto(), solicitud.condiciones()), solicitud.estadoActivo() == null || solicitud.estadoActivo());
		return ResponseEntity.status(HttpStatus.CREATED).body(ProveedorDto.de(proveedor));
	}

	@GetMapping("/{id}")
	@PreAuthorize(ROLES)
	public ProveedorDto consultar(@PathVariable String id) {
		return ProveedorDto.de(proveedores.consultar(Identificadores.numero(id)));
	}

	@PutMapping("/{id}")
	@PreAuthorize(ROLES)
	public ProveedorDto actualizar(@PathVariable String id, @Valid @RequestBody ProveedorActualizacion solicitud) {
		return ProveedorDto.de(proveedores.actualizar(Identificadores.numero(id), new DatosProveedor(solicitud.ruc(),
				solicitud.razonSocial(), solicitud.contacto(), solicitud.condiciones())));
	}

	@PutMapping("/{id}/estado")
	@PreAuthorize(ROLES)
	public ProveedorDto cambiarEstado(@PathVariable String id, @Valid @RequestBody ActualizacionEstado solicitud) {
		return ProveedorDto.de(proveedores.cambiarEstado(Identificadores.numero(id), solicitud.estadoActivo()));
	}

	@GetMapping("/{id}/catalogo")
	@PreAuthorize(ROLES)
	public List<CatalogoItemDto> consultarCatalogo(@PathVariable String id) {
		return proveedores.catalogo(Identificadores.numero(id)).stream().map(CatalogoItemDto::de).toList();
	}

	@PutMapping("/{id}/catalogo")
	@PreAuthorize(ROLES)
	public List<CatalogoItemDto> actualizarCatalogo(@PathVariable String id,
			@Valid @RequestBody CatalogoActualizacion solicitud) {
		List<EntradaCatalogo> entradas = solicitud.productos().stream().map(ProveedorController::entrada).toList();
		return proveedores.actualizarCatalogo(Identificadores.numero(id), entradas).stream()
				.map(CatalogoItemDto::de).toList();
	}

	private static EntradaCatalogo entrada(CatalogoEntrada linea) {
		return new EntradaCatalogo(linea.sku(), linea.precioCompra(), linea.plazoEntregaReferencialDias());
	}
}
