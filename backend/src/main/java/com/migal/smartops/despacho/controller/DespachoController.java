package com.migal.smartops.despacho.controller;

import com.migal.smartops.catalogo.controller.Identificadores;
import com.migal.smartops.comun.idempotencia.ServicioIdempotencia;
import com.migal.smartops.comun.idempotencia.ServicioIdempotencia.Resultado;
import com.migal.smartops.comun.web.Ordenamiento;
import com.migal.smartops.comun.web.PaginaDto;
import com.migal.smartops.despacho.controller.DespachoDtos.GuiaDespachoDto;
import com.migal.smartops.despacho.controller.DespachoDtos.GuiaReimpresionDto;
import com.migal.smartops.despacho.controller.DespachoDtos.GuiaResumenDto;
import com.migal.smartops.despacho.controller.DespachoDtos.ResultadoSalidaDto;
import com.migal.smartops.despacho.controller.DespachoDtos.VentaPendienteDto;
import com.migal.smartops.despacho.controller.SolicitudesDespacho.SolicitudEmisionGuia;
import com.migal.smartops.despacho.controller.SolicitudesDespacho.SolicitudReimpresionGuia;
import com.migal.smartops.despacho.model.EstadoGuia;
import com.migal.smartops.despacho.service.DespachoService;
import com.migal.smartops.seguridad.config.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
 * Despacho (CU-06): listado y consulta de guías, emisión, confirmación de la salida, reimpresión, entrega y ventas
 * pendientes de despacho. La confirmación de la salida exige la cabecera {@code Idempotency-Key}; un reintento con la
 * misma clave devuelve la respuesta original con la cabecera {@code Idempotent-Replayed}.
 */
@RestController
@RequestMapping("/api/v1")
public class DespachoController {

	private static final String ROLES_CONSULTA = "hasAnyRole('ALMACENERO','ADMINISTRADOR')";
	private static final String ROLES_ALMACENERO = "hasRole('ALMACENERO')";
	private static final String ESTADOS = "^(preparacion|emitida|salida_confirmada|entregada)"
			+ "(,(preparacion|emitida|salida_confirmada|entregada))*$";

	private static final Map<String, String> CAMPOS_ORDENABLES = Map.of("fechaEmision", "fechaEmision", "codigo", "codigo");

	private final DespachoService despacho;
	private final ServicioIdempotencia idempotencia;

	public DespachoController(DespachoService despacho, ServicioIdempotencia idempotencia) {
		this.despacho = despacho;
		this.idempotencia = idempotencia;
	}

	@GetMapping("/guias-despacho")
	@PreAuthorize(ROLES_CONSULTA)
	public PaginaDto<GuiaResumenDto> listar(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) @Pattern(regexp = ESTADOS) String estado,
			@RequestParam(required = false) String idVenta,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
			@RequestParam(required = false) String orden) {
		var paginacion = Ordenamiento.paginacion(page, pageSize, orden, CAMPOS_ORDENABLES,
				Sort.by(Sort.Direction.DESC, "fechaEmision"));
		List<EstadoGuia> estados = estado == null ? null
				: Arrays.stream(estado.split(",")).map(EstadoGuia::de).toList();
		Long id = idVenta == null ? null : Identificadores.numero(idVenta);
		return PaginaDto.de(despacho.listar(estados, q, id, desde, hasta, paginacion), GuiaResumenDto::de);
	}

	@PostMapping("/guias-despacho")
	@PreAuthorize(ROLES_ALMACENERO)
	public ResponseEntity<GuiaDespachoDto> emitir(@Valid @RequestBody SolicitudEmisionGuia solicitud) {
		GuiaDespachoDto guia = GuiaDespachoDto.de(despacho.emitir(Identificadores.numero(solicitud.idVenta())));
		return ResponseEntity.status(HttpStatus.CREATED).body(guia);
	}

	@GetMapping("/guias-despacho/{id}")
	@PreAuthorize(ROLES_CONSULTA)
	public GuiaDespachoDto consultar(@PathVariable String id) {
		return GuiaDespachoDto.de(despacho.consultar(Identificadores.numero(id)));
	}

	@GetMapping("/guias-despacho/{id}/documento")
	@PreAuthorize(ROLES_CONSULTA)
	public GuiaDespachoDto documento(@PathVariable String id) {
		return GuiaDespachoDto.de(despacho.consultar(Identificadores.numero(id)));
	}

	@PostMapping("/guias-despacho/{id}/salida")
	@PreAuthorize(ROLES_ALMACENERO)
	public ResponseEntity<ResultadoSalidaDto> confirmarSalida(
			@RequestHeader(value = "Idempotency-Key", required = false) String clave,
			@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable String id) {
		long idGuia = Identificadores.numero(id);
		Resultado<ResultadoSalidaDto> resultado = idempotencia.ejecutar(actor.id(), "POST /guias-despacho/{id}/salida",
				clave, Map.of("id", idGuia), 200, ResultadoSalidaDto.class,
				() -> ResultadoSalidaDto.de(despacho.confirmarSalida(actor.id(), idGuia)));
		return respuesta(resultado);
	}

	@PostMapping("/guias-despacho/{id}/reimpresiones")
	@PreAuthorize(ROLES_ALMACENERO)
	public ResponseEntity<GuiaReimpresionDto> reimprimir(@AuthenticationPrincipal UsuarioAutenticado actor,
			@PathVariable String id, @Valid @RequestBody SolicitudReimpresionGuia solicitud) {
		var reimpresion = despacho.registrarReimpresion(actor.id(), Identificadores.numero(id), solicitud.motivo());
		return ResponseEntity.status(HttpStatus.CREATED).body(GuiaReimpresionDto.de(reimpresion));
	}

	@PostMapping("/guias-despacho/{id}/entrega")
	@PreAuthorize(ROLES_ALMACENERO)
	public GuiaDespachoDto registrarEntrega(@PathVariable String id) {
		return GuiaDespachoDto.de(despacho.registrarEntrega(Identificadores.numero(id)));
	}

	@GetMapping("/ventas-pendientes-despacho")
	@PreAuthorize(ROLES_CONSULTA)
	public PaginaDto<VentaPendienteDto> ventasPendientes(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
		var paginacion = PageRequest.of(page - 1, pageSize,
				Sort.by(Sort.Order.asc("fechaVenta"), Sort.Order.asc("codigo")));
		return PaginaDto.de(despacho.ventasPendientes(q, desde, hasta, paginacion), VentaPendienteDto::de);
	}

	private static <T> ResponseEntity<T> respuesta(Resultado<T> resultado) {
		ResponseEntity.BodyBuilder constructor = ResponseEntity.status(resultado.estado());
		if (resultado.repetida()) {
			constructor.header("Idempotent-Replayed", "true");
		}
		return constructor.body(resultado.cuerpo());
	}
}
