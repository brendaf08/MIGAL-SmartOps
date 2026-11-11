package com.migal.smartops.abastecimiento.controller;

import com.migal.smartops.abastecimiento.controller.AbastecimientoDtos.OrdenCompraDto;
import com.migal.smartops.abastecimiento.controller.SolicitudesAbastecimiento.CierreOrden;
import com.migal.smartops.abastecimiento.controller.SolicitudesAbastecimiento.LineaOrdenSolicitud;
import com.migal.smartops.abastecimiento.controller.SolicitudesAbastecimiento.OrdenActualizacion;
import com.migal.smartops.abastecimiento.controller.SolicitudesAbastecimiento.OrdenRegistro;
import com.migal.smartops.abastecimiento.model.EstadoOrdenCompra;
import com.migal.smartops.abastecimiento.service.OrdenCompraService;
import com.migal.smartops.abastecimiento.service.OrdenCompraService.DatosActualizacionOrden;
import com.migal.smartops.abastecimiento.service.OrdenCompraService.DatosOrden;
import com.migal.smartops.abastecimiento.service.OrdenCompraService.LineaOrden;
import com.migal.smartops.catalogo.controller.Identificadores;
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
import org.springframework.http.HttpStatus;
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
 * Órdenes de compra (CU-12): listado, registro, consulta, modificación y cierre. El cierre exige la cabecera
 * {@code Idempotency-Key}; un reintento con la misma clave devuelve la respuesta original con la cabecera
 * {@code Idempotent-Replayed}.
 */
@RestController
@RequestMapping("/api/v1/ordenes-compra")
public class OrdenCompraController {

	private static final String ROLES_REPONEDOR = "hasRole('REPONEDOR')";
	private static final String ROLES_CONSULTA = "hasAnyRole('REPONEDOR','ALMACENERO','ADMINISTRADOR')";
	private static final String ROLES_LISTADO = "hasAnyRole('REPONEDOR','ADMINISTRADOR')";
	private static final String ESTADOS = "^(pendiente|parcial|completada)(,(pendiente|parcial|completada))*$";
	private static final Map<String, String> CAMPOS_ORDENABLES = Map.of("fechaEmision", "fechaEmision", "codigo", "codigo");

	private final OrdenCompraService ordenes;
	private final ServicioIdempotencia idempotencia;

	public OrdenCompraController(OrdenCompraService ordenes, ServicioIdempotencia idempotencia) {
		this.ordenes = ordenes;
		this.idempotencia = idempotencia;
	}

	@GetMapping
	@PreAuthorize(ROLES_LISTADO)
	public PaginaDto<OrdenCompraDto> listar(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String proveedorId,
			@RequestParam(required = false) @Pattern(regexp = ESTADOS) String estado,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
			@RequestParam(required = false) String orden) {
		var paginacion = Ordenamiento.paginacion(page, pageSize, orden, CAMPOS_ORDENABLES,
				Sort.by(Sort.Direction.DESC, "fechaEmision"));
		List<EstadoOrdenCompra> estados = estado == null ? null
				: Arrays.stream(estado.split(",")).map(EstadoOrdenCompra::de).toList();
		Long idProveedor = proveedorId == null ? null : Identificadores.numero(proveedorId);
		return PaginaDto.de(ordenes.listar(q, idProveedor, estados, desde, hasta, paginacion), OrdenCompraDto::de);
	}

	@PostMapping
	@PreAuthorize(ROLES_REPONEDOR)
	public ResponseEntity<OrdenCompraDto> registrar(@AuthenticationPrincipal UsuarioAutenticado actor,
			@Valid @RequestBody OrdenRegistro solicitud) {
		var creada = ordenes.registrar(actor.id(), new DatosOrden(Identificadores.numero(solicitud.proveedorId()),
				solicitud.fechaEntregaEstimada(), solicitud.observaciones(), solicitud.modalidadPago(),
				convertir(solicitud.lineas())));
		return ResponseEntity.status(HttpStatus.CREATED).body(OrdenCompraDto.de(creada));
	}

	@GetMapping("/{id}")
	@PreAuthorize(ROLES_CONSULTA)
	public OrdenCompraDto consultar(@PathVariable String id) {
		return OrdenCompraDto.de(ordenes.consultar(Identificadores.numero(id)));
	}

	@PutMapping("/{id}")
	@PreAuthorize(ROLES_REPONEDOR)
	public OrdenCompraDto actualizar(@PathVariable String id, @Valid @RequestBody OrdenActualizacion solicitud) {
		return OrdenCompraDto.de(ordenes.actualizar(Identificadores.numero(id), new DatosActualizacionOrden(
				solicitud.fechaEntregaEstimada(), solicitud.observaciones(), solicitud.modalidadPago(),
				convertir(solicitud.lineas()))));
	}

	@PostMapping("/{id}/cierre")
	@PreAuthorize(ROLES_REPONEDOR)
	public ResponseEntity<OrdenCompraDto> cerrar(
			@RequestHeader(value = "Idempotency-Key", required = false) String clave,
			@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable String id,
			@Valid @RequestBody(required = false) CierreOrden solicitud) {
		long idOrden = Identificadores.numero(id);
		String motivo = solicitud == null ? null : solicitud.motivo();
		Resultado<OrdenCompraDto> resultado = idempotencia.ejecutar(actor.id(), "POST /ordenes-compra/{id}/cierre", clave,
				Map.of("id", idOrden, "motivo", String.valueOf(motivo)), 200, OrdenCompraDto.class,
				() -> OrdenCompraDto.de(ordenes.cerrar(idOrden, motivo)));
		return respuesta(resultado);
	}

	private static List<LineaOrden> convertir(List<LineaOrdenSolicitud> lineas) {
		return lineas.stream().map(l -> new LineaOrden(l.sku(), l.cantidadComprada(), l.costoUnitario())).toList();
	}

	private static <T> ResponseEntity<T> respuesta(Resultado<T> resultado) {
		ResponseEntity.BodyBuilder constructor = ResponseEntity.status(resultado.estado());
		if (resultado.repetida()) {
			constructor.header("Idempotent-Replayed", "true");
		}
		return constructor.body(resultado.cuerpo());
	}
}
