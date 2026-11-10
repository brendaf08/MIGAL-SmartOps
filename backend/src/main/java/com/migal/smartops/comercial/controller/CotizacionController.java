package com.migal.smartops.comercial.controller;

import com.migal.smartops.catalogo.controller.Identificadores;
import com.migal.smartops.comercial.controller.ComercialDtos.CotizacionDto;
import com.migal.smartops.comercial.controller.ComercialDtos.DivisionDto;
import com.migal.smartops.comercial.controller.SolicitudesComercial.CotizacionRegistro;
import com.migal.smartops.comercial.controller.SolicitudesComercial.DivisionRegistro;
import com.migal.smartops.comercial.controller.SolicitudesComercial.Referencia;
import com.migal.smartops.comercial.controller.SolicitudesComercial.RechazoRegistro;
import com.migal.smartops.comercial.model.Cotizacion;
import com.migal.smartops.comercial.model.EstadoCotizacion;
import com.migal.smartops.comercial.model.OrigenRechazoCotizacion;
import com.migal.smartops.comercial.service.CotizacionService;
import com.migal.smartops.comun.idempotencia.ServicioIdempotencia;
import com.migal.smartops.comun.idempotencia.ServicioIdempotencia.Resultado;
import com.migal.smartops.comun.web.Ordenamiento;
import com.migal.smartops.comun.web.PaginaDto;
import com.migal.smartops.seguridad.config.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ciclo de vida de las cotizaciones (CU-09). Las transiciones que cambian el estado exigen la cabecera
 * {@code Idempotency-Key} (ADR-009). La validación de viabilidad la registra el Administrador.
 */
@RestController
@RequestMapping("/api/v1/cotizaciones")
public class CotizacionController {

	private static final String ROLES_CONSULTA = "hasAnyRole('VENDEDOR','ADMINISTRADOR')";
	private static final String ROLES_VENDEDOR = "hasRole('VENDEDOR')";
	private static final String ROLES_ADMINISTRADOR = "hasRole('ADMINISTRADOR')";
	private static final String ROLES_RECHAZO = "hasAnyRole('VENDEDOR','ADMINISTRADOR')";
	private static final String ESTADOS = "^(emitida|aceptada|validada|rechazada|vencida)(,(emitida|aceptada|validada|rechazada|vencida))*$";

	private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
			"fechaEmision", "fechaEmision",
			"fechaVencimiento", "fechaVencimiento");

	private final CotizacionService cotizaciones;
	private final ServicioIdempotencia idempotencia;

	public CotizacionController(CotizacionService cotizaciones, ServicioIdempotencia idempotencia) {
		this.cotizaciones = cotizaciones;
		this.idempotencia = idempotencia;
	}

	@GetMapping
	@PreAuthorize(ROLES_CONSULTA)
	public PaginaDto<CotizacionDto> listar(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) @Pattern(regexp = ESTADOS) String estado,
			@RequestParam(required = false) String clienteId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
			@RequestParam(required = false) String orden) {
		Pageable paginacion = Ordenamiento.paginacion(page, pageSize, orden, CAMPOS_ORDENABLES,
				Sort.by(Sort.Direction.DESC, "fechaEmision"));
		List<EstadoCotizacion> estados = estado == null ? null
				: Arrays.stream(estado.split(",")).map(EstadoCotizacion::de).toList();
		Long idCliente = clienteId == null ? null : Identificadores.numero(clienteId);
		return PaginaDto.de(cotizaciones.listar(q, estados, idCliente, desde, hasta, paginacion), CotizacionDto::de);
	}

	@PostMapping
	@PreAuthorize(ROLES_VENDEDOR)
	public ResponseEntity<CotizacionDto> crear(@AuthenticationPrincipal UsuarioAutenticado actor,
			@Valid @RequestBody CotizacionRegistro solicitud) {
		Cotizacion creada = cotizaciones.crear(solicitud.datos(), actor.id());
		return ResponseEntity.created(URI.create("/api/v1/cotizaciones/" + creada.getId()))
				.body(CotizacionDto.de(creada));
	}

	@GetMapping("/{id}")
	@PreAuthorize(ROLES_CONSULTA)
	public CotizacionDto consultar(@PathVariable String id) {
		return CotizacionDto.de(cotizaciones.consultar(Identificadores.numero(id)));
	}

	@PutMapping("/{id}")
	@PreAuthorize(ROLES_VENDEDOR)
	public CotizacionDto actualizar(@PathVariable String id, @Valid @RequestBody CotizacionRegistro solicitud) {
		return CotizacionDto.de(cotizaciones.actualizar(Identificadores.numero(id), solicitud.datos()));
	}

	@PostMapping("/{id}/aceptacion")
	@PreAuthorize(ROLES_VENDEDOR)
	public ResponseEntity<CotizacionDto> registrarAceptacion(
			@RequestHeader(value = "Idempotency-Key", required = false) String clave,
			@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable String id) {
		long idCotizacion = Identificadores.numero(id);
		Resultado<CotizacionDto> resultado = idempotencia.ejecutar(actor.id(),
				"POST /cotizaciones/{id}/aceptacion", clave, new Referencia(idCotizacion), 200, CotizacionDto.class,
				() -> CotizacionDto.de(cotizaciones.registrarAceptacion(idCotizacion)));
		return respuesta(resultado);
	}

	@PostMapping("/{id}/validacion")
	@PreAuthorize(ROLES_ADMINISTRADOR)
	public ResponseEntity<CotizacionDto> registrarValidacion(
			@RequestHeader(value = "Idempotency-Key", required = false) String clave,
			@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable String id) {
		long idCotizacion = Identificadores.numero(id);
		Resultado<CotizacionDto> resultado = idempotencia.ejecutar(actor.id(),
				"POST /cotizaciones/{id}/validacion", clave, new Referencia(idCotizacion), 200, CotizacionDto.class,
				() -> CotizacionDto.de(cotizaciones.registrarValidacion(idCotizacion)));
		return respuesta(resultado);
	}

	@PostMapping("/{id}/rechazo")
	@PreAuthorize(ROLES_RECHAZO)
	public ResponseEntity<CotizacionDto> registrarRechazo(
			@RequestHeader(value = "Idempotency-Key", required = false) String clave,
			@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable String id,
			@Valid @RequestBody RechazoRegistro solicitud) {
		long idCotizacion = Identificadores.numero(id);
		OrigenRechazoCotizacion origen = OrigenRechazoCotizacion.de(solicitud.origen());
		Resultado<CotizacionDto> resultado = idempotencia.ejecutar(actor.id(),
				"POST /cotizaciones/{id}/rechazo", clave, Map.of("id", idCotizacion, "solicitud", solicitud), 200,
				CotizacionDto.class,
				() -> CotizacionDto.de(cotizaciones.registrarRechazo(idCotizacion, origen, solicitud.motivo(),
						actor.id(), actor.rol())));
		return respuesta(resultado);
	}

	@PostMapping("/{id}/division")
	@PreAuthorize(ROLES_VENDEDOR)
	public ResponseEntity<DivisionDto> dividir(
			@RequestHeader(value = "Idempotency-Key", required = false) String clave,
			@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable String id,
			@Valid @RequestBody DivisionRegistro solicitud) {
		long idCotizacion = Identificadores.numero(id);
		Resultado<DivisionDto> resultado = idempotencia.ejecutar(actor.id(), "POST /cotizaciones/{id}/division", clave,
				Map.of("id", idCotizacion, "solicitud", solicitud), 201, DivisionDto.class,
				() -> DivisionDto.de(cotizaciones.dividir(idCotizacion,
						solicitud.lineas().stream().map(SolicitudesComercial.LineaDivisionRegistro::datos).toList())));
		return respuesta(resultado);
	}

	@PostMapping("/{id}/vencimiento")
	@PreAuthorize(ROLES_CONSULTA)
	public ResponseEntity<CotizacionDto> registrarVencimiento(
			@RequestHeader(value = "Idempotency-Key", required = false) String clave,
			@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable String id) {
		long idCotizacion = Identificadores.numero(id);
		Resultado<CotizacionDto> resultado = idempotencia.ejecutar(actor.id(),
				"POST /cotizaciones/{id}/vencimiento", clave, new Referencia(idCotizacion), 200, CotizacionDto.class,
				() -> CotizacionDto.de(cotizaciones.registrarVencimiento(idCotizacion)));
		return respuesta(resultado);
	}

	private static <T> ResponseEntity<T> respuesta(Resultado<T> resultado) {
		ResponseEntity.BodyBuilder constructor = ResponseEntity.status(resultado.estado());
		if (resultado.repetida()) {
			constructor.header("Idempotent-Replayed", "true");
		}
		return constructor.body(resultado.cuerpo());
	}
}
