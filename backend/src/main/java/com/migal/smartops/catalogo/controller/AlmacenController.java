package com.migal.smartops.catalogo.controller;

import com.migal.smartops.catalogo.controller.SolicitudesCatalogo.ActualizacionEstado;
import com.migal.smartops.catalogo.service.AlmacenService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tienda principal y almacenes. La lista la consultan los cuatro roles; la activación la gestiona solo el
 * Administrador (asignación pendiente de confirmación según el contrato).
 */
@RestController
@RequestMapping("/api/v1/almacenes")
public class AlmacenController {

	private final AlmacenService almacenes;

	public AlmacenController(AlmacenService almacenes) {
		this.almacenes = almacenes;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ALMACENERO','VENDEDOR','REPONEDOR','ADMINISTRADOR')")
	public List<AlmacenDto> listar() {
		return almacenes.listar().stream().map(AlmacenDto::de).toList();
	}

	@PutMapping("/{id}/estado")
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	public AlmacenDto cambiarEstado(@PathVariable String id, @Valid @RequestBody ActualizacionEstado solicitud) {
		return AlmacenDto.de(almacenes.cambiarEstado(Identificadores.numero(id), solicitud.estadoActivo()));
	}
}
