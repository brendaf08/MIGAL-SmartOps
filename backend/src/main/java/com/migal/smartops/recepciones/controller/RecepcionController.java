package com.migal.smartops.recepciones.controller;

import com.migal.smartops.catalogo.controller.Identificadores;
import com.migal.smartops.comun.idempotencia.ServicioIdempotencia;
import com.migal.smartops.comun.idempotencia.ServicioIdempotencia.Resultado;
import com.migal.smartops.comun.web.Ordenamiento;
import com.migal.smartops.comun.web.PaginaDto;
import com.migal.smartops.recepciones.controller.RecepcionesDtos.DetalleDto;
import com.migal.smartops.recepciones.controller.RecepcionesDtos.ResultadoDto;
import com.migal.smartops.recepciones.controller.RecepcionesDtos.ResumenDto;
import com.migal.smartops.recepciones.controller.SolicitudesRecepciones.LineaSolicitud;
import com.migal.smartops.recepciones.controller.SolicitudesRecepciones.RecepcionActualizacion;
import com.migal.smartops.recepciones.controller.SolicitudesRecepciones.RecepcionRegistro;
import com.migal.smartops.recepciones.controller.SolicitudesRecepciones.RecepcionValidacion;
import com.migal.smartops.recepciones.model.EstadoRecepcion;
import com.migal.smartops.recepciones.service.RecepcionService;
import com.migal.smartops.recepciones.service.RecepcionService.DatosActualizacionRecepcion;
import com.migal.smartops.recepciones.service.RecepcionService.DatosRecepcion;
import com.migal.smartops.recepciones.service.RecepcionService.LineaRecepcion;
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
 * Recepciones de mercadería (CU-13): listado, registro, consulta, actualización y validación. La validación exige la
 * cabecera {@code Idempotency-Key}; un reintento con la misma clave devuelve la respuesta original con la cabecera
 * {@code Idempotent-Replayed}.
 */
@RestController
@RequestMapping("/api/v1/recepciones")
public class RecepcionController {

	private static final String ROLES_CONSULTA = "hasAnyRole('ALMACENERO','ADMINISTRADOR')";
	private static final String ROLES_ALMACENERO = "hasRole('ALMACENERO')";
	private static final String ESTADOS = "^(pendiente|validada)(,(pendiente|validada))*$";
	private static final Map<String, String> CAMPOS_ORDENABLES = Map.of("fechaRecepcion", "fechaRecepcion", "codigo", "codigo");

	private final RecepcionService recepciones;
	private final ServicioIdempotencia idempotencia;

	public RecepcionController(RecepcionService recepciones, ServicioIdempotencia idempotencia) {
		this.recepciones = recepciones;
		this.idempotencia = idempotencia;
	}

	@GetMapping
	@PreAuthorize(ROLES_CONSULTA)
	public PaginaDto<ResumenDto> listar(
			@RequestParam(required = false) String ordenCompraId,
			@RequestParam(required = false) @Pattern(regexp = ESTADOS) String estado,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(required = false) String orden,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
		var paginacion = Ordenamiento.paginacion(page, pageSize, orden, CAMPOS_ORDENABLES,
				Sort.by(Sort.Direction.DESC, "fechaRecepcion"));
		List<EstadoRecepcion> estados = estado == null ? null
				: Arrays.stream(estado.split(",")).map(EstadoRecepcion::de).toList();
		Long idOrden = ordenCompraId == null ? null : Identificadores.numero(ordenCompraId);
		return PaginaDto.de(recepciones.listar(idOrden, estados, desde, hasta, paginacion), ResumenDto::de);
	}

	@PostMapping
	@PreAuthorize(ROLES_ALMACENERO)
	public ResponseEntity<DetalleDto> registrar(@AuthenticationPrincipal UsuarioAutenticado actor,
			@Valid @RequestBody RecepcionRegistro solicitud) {
		var creada = recepciones.registrar(actor.id(), new DatosRecepcion(Identificadores.numero(solicitud.ordenCompraId()),
				solicitud.fechaRecepcion(), solicitud.observaciones(),
				solicitud.documentoId() == null ? null : Identificadores.numero(solicitud.documentoId()),
				solicitud.ubicacionPrecargaId() == null ? null : Identificadores.numero(solicitud.ubicacionPrecargaId()),
				convertir(solicitud.lineas())));
		return ResponseEntity.status(HttpStatus.CREATED).body(DetalleDto.de(creada));
	}

	@GetMapping("/{id}")
	@PreAuthorize(ROLES_CONSULTA)
	public DetalleDto consultar(@PathVariable String id) {
		return DetalleDto.de(recepciones.consultar(Identificadores.numero(id)));
	}

	@PutMapping("/{id}")
	@PreAuthorize(ROLES_ALMACENERO)
	public DetalleDto actualizar(@PathVariable String id, @Valid @RequestBody RecepcionActualizacion solicitud) {
		return DetalleDto.de(recepciones.actualizar(Identificadores.numero(id), new DatosActualizacionRecepcion(
				solicitud.fechaRecepcion(), solicitud.observaciones(), convertir(solicitud.lineas()))));
	}

	@PostMapping("/{id}/validacion")
	@PreAuthorize(ROLES_ALMACENERO)
	public ResponseEntity<ResultadoDto> validar(
			@RequestHeader(value = "Idempotency-Key", required = false) String clave,
			@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable String id,
			@Valid @RequestBody(required = false) RecepcionValidacion solicitud) {
		long idRecepcion = Identificadores.numero(id);
		String observaciones = solicitud == null ? null : solicitud.observaciones();
		Resultado<ResultadoDto> resultado = idempotencia.ejecutar(actor.id(), "POST /recepciones/{id}/validacion", clave,
				Map.of("id", idRecepcion, "observaciones", String.valueOf(observaciones)), 200, ResultadoDto.class,
				() -> ResultadoDto.de(recepciones.validar(actor.id(), idRecepcion, observaciones, observaciones != null)));
		return respuesta(resultado);
	}

	private static List<LineaRecepcion> convertir(List<LineaSolicitud> lineas) {
		if (lineas == null) {
			return null;
		}
		return lineas.stream().map(l -> new LineaRecepcion(Identificadores.numero(l.productoId()),
				Identificadores.numero(l.ubicacionId()), l.cantidadRecibida())).toList();
	}

	private static <T> ResponseEntity<T> respuesta(Resultado<T> resultado) {
		ResponseEntity.BodyBuilder constructor = ResponseEntity.status(resultado.estado());
		if (resultado.repetida()) {
			constructor.header("Idempotent-Replayed", "true");
		}
		return constructor.body(resultado.cuerpo());
	}
}
