package com.migal.smartops.comercial.controller;

import com.migal.smartops.catalogo.controller.Identificadores;
import com.migal.smartops.comercial.controller.ComercialDtos.ClienteDto;
import com.migal.smartops.comercial.controller.SolicitudesComercial.ClienteEstado;
import com.migal.smartops.comercial.controller.SolicitudesComercial.ClienteRegistro;
import com.migal.smartops.comercial.model.Cliente;
import com.migal.smartops.comercial.service.ClienteService;
import com.migal.smartops.comun.web.Ordenamiento;
import com.migal.smartops.comun.web.PaginaDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.Map;
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
 * Gestión de clientes (módulo comercial). Lo utilizan el Vendedor y el Administrador, según el contrato
 * {@code comercial.yaml}.
 */
@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {

	private static final String ROLES = "hasAnyRole('VENDEDOR','ADMINISTRADOR')";

	private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
			"nombreRazonSocial", "nombreRazonSocial",
			"fechaRegistro", "fechaRegistro");

	private final ClienteService clientes;

	public ClienteController(ClienteService clientes) {
		this.clientes = clientes;
	}

	@GetMapping
	@PreAuthorize(ROLES)
	public PaginaDto<ClienteDto> listar(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) Boolean estadoActivo,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
			@RequestParam(required = false) String orden) {
		Pageable paginacion = Ordenamiento.paginacion(page, pageSize, orden, CAMPOS_ORDENABLES,
				Sort.by("nombreRazonSocial"));
		return PaginaDto.de(clientes.listar(q, estadoActivo, paginacion), ClienteDto::de);
	}

	@PostMapping
	@PreAuthorize(ROLES)
	public ResponseEntity<ClienteDto> registrar(@Valid @RequestBody ClienteRegistro solicitud) {
		Cliente creado = clientes.registrar(solicitud.datos());
		return ResponseEntity.created(URI.create("/api/v1/clientes/" + creado.getId()))
				.body(ClienteDto.de(creado));
	}

	@GetMapping("/{id}")
	@PreAuthorize(ROLES)
	public ClienteDto consultar(@PathVariable String id) {
		return ClienteDto.de(clientes.consultar(Identificadores.numero(id)));
	}

	@PutMapping("/{id}")
	@PreAuthorize(ROLES)
	public ClienteDto actualizar(@PathVariable String id, @Valid @RequestBody ClienteRegistro solicitud) {
		return ClienteDto.de(clientes.actualizar(Identificadores.numero(id), solicitud.datos()));
	}

	@PutMapping("/{id}/estado")
	@PreAuthorize(ROLES)
	public ClienteDto cambiarEstado(@PathVariable String id, @Valid @RequestBody ClienteEstado solicitud) {
		return ClienteDto.de(clientes.cambiarEstado(Identificadores.numero(id), solicitud.estadoActivo()));
	}
}
