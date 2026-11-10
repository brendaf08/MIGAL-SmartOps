package com.migal.smartops.inventario.controller;

import com.migal.smartops.catalogo.controller.Identificadores;
import com.migal.smartops.comun.idempotencia.ServicioIdempotencia;
import com.migal.smartops.comun.idempotencia.ServicioIdempotencia.Resultado;
import com.migal.smartops.comun.web.PaginaDto;
import com.migal.smartops.inventario.controller.InventarioDtos.AlertaStockDto;
import com.migal.smartops.inventario.controller.InventarioDtos.DisponibilidadPaginaDto;
import com.migal.smartops.inventario.controller.InventarioDtos.MovimientoKardexDto;
import com.migal.smartops.inventario.controller.InventarioDtos.ProductoExistenciasDto;
import com.migal.smartops.inventario.controller.InventarioDtos.ResultadoMovimientoDto;
import com.migal.smartops.inventario.controller.InventarioDtos.ResultadoTrasladoDto;
import com.migal.smartops.inventario.controller.SolicitudesInventario.SolicitudMovimiento;
import com.migal.smartops.inventario.controller.SolicitudesInventario.SolicitudTrasladoDto;
import com.migal.smartops.inventario.service.ConsultaInventarioService;
import com.migal.smartops.inventario.service.ConsultaInventarioService.Disponibilidad;
import com.migal.smartops.inventario.service.MovimientoInventarioService;
import com.migal.smartops.seguridad.config.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
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
 * Inventario (CU-04, CU-05 y CU-07): disponibilidad, existencias, alertas, Kardex, movimientos manuales y
 * traslados. Las operaciones de escritura exigen la cabecera {@code Idempotency-Key}; un reintento con la misma
 * clave devuelve la respuesta original con la cabecera {@code Idempotent-Replayed}.
 */
@RestController
@RequestMapping("/api/v1")
public class InventarioController {

	private static final String ROLES_CONSULTA = "hasAnyRole('ALMACENERO','VENDEDOR','REPONEDOR','ADMINISTRADOR')";
	private static final String ROLES_ALERTAS = "hasAnyRole('ALMACENERO','REPONEDOR','ADMINISTRADOR')";
	private static final String ROLES_KARDEX = "hasAnyRole('ALMACENERO','ADMINISTRADOR')";
	private static final String ROLES_TRASLADO = "hasAnyRole('ADMINISTRADOR','ALMACENERO')";

	private final ConsultaInventarioService consultas;
	private final MovimientoInventarioService movimientos;
	private final ServicioIdempotencia idempotencia;

	public InventarioController(ConsultaInventarioService consultas, MovimientoInventarioService movimientos,
			ServicioIdempotencia idempotencia) {
		this.consultas = consultas;
		this.movimientos = movimientos;
		this.idempotencia = idempotencia;
	}

	@GetMapping("/inventario/disponibilidad")
	@PreAuthorize(ROLES_CONSULTA)
	public DisponibilidadPaginaDto disponibilidad(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String sku,
			@RequestParam(required = false) Long categoriaId,
			@RequestParam(defaultValue = "false") boolean soloAlerta,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
		Disponibilidad d = consultas.disponibilidad(q, sku, categoriaId, soloAlerta, page, pageSize);
		PaginaDto<ProductoExistenciasDto> pagina = PaginaDto.de(d.pagina(), ProductoExistenciasDto::de);
		return new DisponibilidadPaginaDto(pagina.items(), pagina.total(), pagina.page(), pagina.pageSize(),
				d.enAlerta(), d.totalGeneral());
	}

	@GetMapping("/productos/{id}/existencias")
	@PreAuthorize(ROLES_CONSULTA)
	public ProductoExistenciasDto existencias(@PathVariable String id) {
		return ProductoExistenciasDto.de(consultas.existencias(Identificadores.numero(id)));
	}

	@GetMapping("/inventario/alertas")
	@PreAuthorize(ROLES_ALERTAS)
	public PaginaDto<AlertaStockDto> alertas(
			@RequestParam(required = false) String nivel,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
		return PaginaDto.de(consultas.alertas(nivel, page, pageSize), AlertaStockDto::de);
	}

	@GetMapping("/inventario/movimientos")
	@PreAuthorize(ROLES_KARDEX)
	public PaginaDto<MovimientoKardexDto> kardex(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String sku,
			@RequestParam(required = false) Long ubicacionId,
			@RequestParam(required = false) String tipoOperacion,
			@RequestParam(required = false) String origen,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
		return PaginaDto.de(consultas.kardex(q, sku, ubicacionId, tipoOperacion, origen, desde, hasta, page, pageSize),
				MovimientoKardexDto::de);
	}

	@PostMapping("/inventario/movimientos")
	@PreAuthorize("hasRole('ALMACENERO')")
	public ResponseEntity<ResultadoMovimientoDto> registrarMovimiento(
			@RequestHeader(value = "Idempotency-Key", required = false) String clave,
			@AuthenticationPrincipal UsuarioAutenticado actor,
			@Valid @RequestBody SolicitudMovimiento solicitud) {
		Resultado<ResultadoMovimientoDto> resultado = idempotencia.ejecutar(actor.id(), "POST /inventario/movimientos",
				clave, solicitud, 201, ResultadoMovimientoDto.class,
				() -> ResultadoMovimientoDto.de(movimientos.registrarAjuste(actor.id(), solicitud.aServicio())));
		return respuesta(resultado);
	}

	@PostMapping("/inventario/traslados")
	@PreAuthorize(ROLES_TRASLADO)
	public ResponseEntity<ResultadoTrasladoDto> registrarTraslado(
			@RequestHeader(value = "Idempotency-Key", required = false) String clave,
			@AuthenticationPrincipal UsuarioAutenticado actor,
			@Valid @RequestBody SolicitudTrasladoDto solicitud) {
		Resultado<ResultadoTrasladoDto> resultado = idempotencia.ejecutar(actor.id(), "POST /inventario/traslados",
				clave, solicitud, 201, ResultadoTrasladoDto.class,
				() -> ResultadoTrasladoDto.de(movimientos.registrarTraslado(actor.id(), solicitud.aServicio())));
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
