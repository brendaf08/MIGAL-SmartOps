package com.migal.smartops.comercial.controller;

import com.migal.smartops.catalogo.controller.Identificadores;
import com.migal.smartops.comercial.controller.SolicitudesVenta.SolicitudCancelacionVenta;
import com.migal.smartops.comercial.controller.SolicitudesVenta.SolicitudVentaDesdeCotizacion;
import com.migal.smartops.comercial.controller.SolicitudesVenta.SolicitudVentaDirecta;
import com.migal.smartops.comercial.controller.VentaDtos.VentaDto;
import com.migal.smartops.comercial.controller.VentaDtos.VentaResumenDto;
import com.migal.smartops.comercial.model.EstadoVenta;
import com.migal.smartops.comercial.service.VentaService;
import com.migal.smartops.comun.idempotencia.ServicioIdempotencia;
import com.migal.smartops.comun.idempotencia.ServicioIdempotencia.Resultado;
import com.migal.smartops.comun.web.Ordenamiento;
import com.migal.smartops.comun.web.PaginaDto;
import com.migal.smartops.seguridad.config.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ventas (CU-10): listado, consulta, registro desde cotización o de mostrador, cobertura y cancelación. Las operaciones
 * que modifican reservas o estado exigen la cabecera {@code Idempotency-Key}; un reintento con la misma clave devuelve
 * la respuesta original con la cabecera {@code Idempotent-Replayed}.
 */
@RestController
@RequestMapping("/api/v1/ventas")
public class VentaController {

	private static final String ROLES_CONSULTA = "hasAnyRole('ALMACENERO','VENDEDOR','ADMINISTRADOR')";
	private static final String ROLES_COBERTURA = "hasAnyRole('ALMACENERO','VENDEDOR')";
	private static final String ROLES_VENDEDOR = "hasRole('VENDEDOR')";
	private static final String ROLES_DIRECTA = "hasAnyRole('VENDEDOR','ADMINISTRADOR')";
	private static final String ESTADOS = "^(pendiente_abastecimiento|pendiente_despacho|lista_para_despacho|despachada|cancelada)"
			+ "(,(pendiente_abastecimiento|pendiente_despacho|lista_para_despacho|despachada|cancelada))*$";

	private static final Map<String, String> CAMPOS_ORDENABLES = Map.of("fechaVenta", "fechaVenta");

	private final VentaService ventas;
	private final ServicioIdempotencia idempotencia;

	public VentaController(VentaService ventas, ServicioIdempotencia idempotencia) {
		this.ventas = ventas;
		this.idempotencia = idempotencia;
	}

	@GetMapping
	@PreAuthorize(ROLES_CONSULTA)
	public PaginaDto<VentaResumenDto> listar(
			@RequestParam(required = false) @Pattern(regexp = ESTADOS) String estado,
			@RequestParam(required = false) String clienteId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
			@RequestParam(required = false) String orden) {
		var paginacion = Ordenamiento.paginacion(page, pageSize, orden, CAMPOS_ORDENABLES,
				Sort.by(Sort.Direction.DESC, "fechaVenta"));
		List<EstadoVenta> estados = estado == null ? null
				: Arrays.stream(estado.split(",")).map(EstadoVenta::de).toList();
		Long idCliente = clienteId == null ? null : Identificadores.numero(clienteId);
		return PaginaDto.de(ventas.listar(estados, idCliente, desde, hasta, paginacion), VentaResumenDto::de);
	}

	@PostMapping
	@PreAuthorize(ROLES_VENDEDOR)
	public ResponseEntity<VentaDto> registrarDesdeCotizacion(
			@RequestHeader(value = "Idempotency-Key", required = false) String clave,
			@AuthenticationPrincipal UsuarioAutenticado actor,
			@Valid @RequestBody SolicitudVentaDesdeCotizacion solicitud) {
		long idCotizacion = Identificadores.numero(solicitud.idCotizacion());
		Resultado<VentaDto> resultado = idempotencia.ejecutar(actor.id(), "POST /ventas", clave, solicitud, 201,
				VentaDto.class, () -> VentaDto.de(ventas.registrarDesdeCotizacion(actor.id(), idCotizacion)));
		return respuesta(resultado);
	}

	@PostMapping("/directas")
	@PreAuthorize(ROLES_DIRECTA)
	public ResponseEntity<VentaDto> registrarDirecta(
			@RequestHeader(value = "Idempotency-Key", required = false) String clave,
			@AuthenticationPrincipal UsuarioAutenticado actor,
			@Valid @RequestBody SolicitudVentaDirecta solicitud) {
		Resultado<VentaDto> resultado = idempotencia.ejecutar(actor.id(), "POST /ventas/directas", clave, solicitud, 201,
				VentaDto.class, () -> VentaDto.de(ventas.registrarDirecta(actor.id(), actor.rol(), solicitud.datos())));
		return respuesta(resultado);
	}

	@GetMapping("/{id}")
	@PreAuthorize(ROLES_CONSULTA)
	public VentaDto consultar(@PathVariable String id) {
		return VentaDto.de(ventas.consultar(Identificadores.numero(id)));
	}

	@PostMapping("/{id}/cobertura")
	@PreAuthorize(ROLES_COBERTURA)
	public ResponseEntity<VentaDto> reintentarCobertura(
			@RequestHeader(value = "Idempotency-Key", required = false) String clave,
			@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable String id) {
		long idVenta = Identificadores.numero(id);
		Resultado<VentaDto> resultado = idempotencia.ejecutar(actor.id(), "POST /ventas/{id}/cobertura", clave,
				Map.of("id", idVenta), 200, VentaDto.class,
				() -> VentaDto.de(ventas.reintentarCobertura(actor.id(), idVenta)));
		return respuesta(resultado);
	}

	@PostMapping("/{id}/cancelacion")
	@PreAuthorize(ROLES_VENDEDOR)
	public ResponseEntity<VentaDto> cancelar(
			@RequestHeader(value = "Idempotency-Key", required = false) String clave,
			@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable String id,
			@Valid @RequestBody SolicitudCancelacionVenta solicitud) {
		long idVenta = Identificadores.numero(id);
		Resultado<VentaDto> resultado = idempotencia.ejecutar(actor.id(), "POST /ventas/{id}/cancelacion", clave,
				Map.of("id", idVenta, "solicitud", solicitud), 200, VentaDto.class,
				() -> VentaDto.de(ventas.cancelar(actor.id(), idVenta, solicitud.motivo())));
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
