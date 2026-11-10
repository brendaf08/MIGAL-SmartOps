package com.migal.smartops.documentosocr.controller;

import com.migal.smartops.catalogo.controller.Identificadores;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.comun.web.Ordenamiento;
import com.migal.smartops.comun.web.PaginaDto;
import com.migal.smartops.comun.web.RespuestaProblema.ErrorCampo;
import com.migal.smartops.documentosocr.config.PropiedadesOcr;
import com.migal.smartops.documentosocr.controller.DocumentosOcrDtos.DetalleDto;
import com.migal.smartops.documentosocr.controller.DocumentosOcrDtos.DocumentoDto;
import com.migal.smartops.documentosocr.controller.DocumentosOcrDtos.ResumenDto;
import com.migal.smartops.documentosocr.controller.SolicitudesDocumentosOcr.AsociacionSolicitud;
import com.migal.smartops.documentosocr.controller.SolicitudesDocumentosOcr.ResultadosSolicitud;
import com.migal.smartops.documentosocr.model.DetalleExtraido;
import com.migal.smartops.documentosocr.model.DocumentoDigital;
import com.migal.smartops.documentosocr.model.EstadoProcesamiento;
import com.migal.smartops.documentosocr.model.TipoDocumento;
import com.migal.smartops.documentosocr.service.DocumentoOcrService;
import com.migal.smartops.seguridad.config.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Documentos digitales OCR (CU-08): carga, consulta, revisión, validación, asociación y descarte. Cada operación exige el
 * rol del contrato; la restricción por tipo de documento la aplica el servicio. La recepción de resultados solo la invoca el
 * servicio de OCR con su alcance {@code ocr:resultados:escribir}.
 */
@RestController
@RequestMapping("/api/v1/documentos")
public class DocumentoOcrController {

	private static final String ROLES_OPERACION = "hasAnyRole('VENDEDOR','ALMACENERO','ADMINISTRADOR')";
	private static final String ALCANCE_RESULTADOS = "hasAuthority('SCOPE_ocr:resultados:escribir')";
	private static final Map<String, String> CAMPOS_ORDENABLES = Map.of("fechaSubida", "fechaSubida");

	private final DocumentoOcrService documentos;
	private final PropiedadesOcr propiedades;

	public DocumentoOcrController(DocumentoOcrService documentos, PropiedadesOcr propiedades) {
		this.documentos = documentos;
		this.propiedades = propiedades;
	}

	@PostMapping(consumes = "multipart/form-data")
	@PreAuthorize(ROLES_OPERACION)
	public ResponseEntity<ResumenDto> cargar(@AuthenticationPrincipal UsuarioAutenticado actor,
			@RequestParam("archivo") MultipartFile archivo, @RequestParam("tipoDocumento") String tipoDocumento) {
		TipoDocumento tipo = tipoDe(tipoDocumento);
		byte[] contenido = contenido(archivo);
		DocumentoDigital creado = documentos.cargar(actor, tipo, archivo.getOriginalFilename(), contenido);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(ResumenDto.de(creado));
	}

	@GetMapping
	@PreAuthorize(ROLES_OPERACION)
	public PaginaDto<ResumenDto> listar(@AuthenticationPrincipal UsuarioAutenticado actor,
			@RequestParam(required = false) String estadoProcesamiento,
			@RequestParam(required = false) String tipoDocumento,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(required = false) String orden,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
		var paginacion = Ordenamiento.paginacion(page, pageSize, orden, CAMPOS_ORDENABLES,
				Sort.by(Sort.Direction.DESC, "fechaSubida"));
		EstadoProcesamiento estado = estadoProcesamiento == null ? null : estadoDe(estadoProcesamiento);
		TipoDocumento tipo = tipoDocumento == null ? null : tipoDe(tipoDocumento);
		return PaginaDto.de(documentos.listar(actor.rol(), estado, tipo, desde, hasta, paginacion), ResumenDto::de);
	}

	@GetMapping("/{id}")
	@PreAuthorize(ROLES_OPERACION)
	public DocumentoDto consultar(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable String id) {
		return respuesta(documentos.consultar(actor, Identificadores.numero(id)));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize(ROLES_OPERACION)
	public ResponseEntity<Void> descartar(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable String id) {
		documentos.descartar(actor, Identificadores.numero(id));
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{id}/resultados")
	@PreAuthorize(ALCANCE_RESULTADOS)
	public DocumentoDto registrarResultados(@PathVariable String id, @Valid @RequestBody ResultadosSolicitud solicitud) {
		return respuesta(documentos.registrarResultados(Identificadores.numero(id), solicitud.aResultado()));
	}

	@PutMapping("/{id}/detalles/{idDetalle}")
	@PreAuthorize(ROLES_OPERACION)
	public DetalleDto corregirDetalle(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable String id,
			@PathVariable String idDetalle, @RequestBody Map<String, Object> cuerpo) {
		DetalleExtraido linea = documentos.corregirDetalle(actor, Identificadores.numero(id),
				Identificadores.numero(idDetalle), SolicitudesDocumentosOcr.aCorreccion(cuerpo));
		return DetalleDto.de(linea);
	}

	@PostMapping("/{id}/detalles/{idDetalle}/descarte")
	@PreAuthorize(ROLES_OPERACION)
	public DocumentoDto descartarDetalle(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable String id,
			@PathVariable String idDetalle) {
		return respuesta(documentos.descartarDetalle(actor, Identificadores.numero(id), Identificadores.numero(idDetalle)));
	}

	@PostMapping("/{id}/validacion")
	@PreAuthorize(ROLES_OPERACION)
	public DocumentoDto validar(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable String id) {
		return respuesta(documentos.validar(actor, Identificadores.numero(id)));
	}

	@PostMapping("/{id}/asociacion")
	@PreAuthorize(ROLES_OPERACION)
	public DocumentoDto asociar(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable String id,
			@Valid @RequestBody AsociacionSolicitud solicitud) {
		return respuesta(documentos.asociar(actor, Identificadores.numero(id),
				solicitud.idCotizacion() == null ? null : Identificadores.numero(solicitud.idCotizacion()),
				solicitud.idRecepcion() == null ? null : Identificadores.numero(solicitud.idRecepcion())));
	}

	@PostMapping("/{id}/procesamiento")
	@PreAuthorize(ROLES_OPERACION)
	public ResponseEntity<DocumentoDto> reprocesar(@AuthenticationPrincipal UsuarioAutenticado actor,
			@PathVariable String id) {
		return ResponseEntity.accepted().body(respuesta(documentos.reprocesar(actor, Identificadores.numero(id))));
	}

	private DocumentoDto respuesta(DocumentoDigital documento) {
		return DocumentoDto.de(documento, propiedades.umbrales());
	}

	private static TipoDocumento tipoDe(String valor) {
		TipoDocumento tipo = TipoDocumento.de(valor);
		if (tipo == null) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "El tipo de documento no es válido.",
					Map.of("errores", List.of(new ErrorCampo("tipoDocumento", "valor_no_admitido",
							"Valores admitidos: solicitud_cliente, documento_recepcion."))));
		}
		return tipo;
	}

	private static EstadoProcesamiento estadoDe(String valor) {
		for (EstadoProcesamiento estado : EstadoProcesamiento.values()) {
			if (estado.valor().equals(valor)) {
				return estado;
			}
		}
		throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "El estado de procesamiento no es válido.",
				Map.of("errores", List.of(new ErrorCampo("estadoProcesamiento", "valor_no_admitido",
						"Valores admitidos: pendiente, procesado_ocr, validado."))));
	}

	private static byte[] contenido(MultipartFile archivo) {
		try {
			return archivo.getBytes();
		} catch (IOException e) {
			throw new ErrorDeNegocio(CodigoError.SERVICIO_NO_DISPONIBLE, "No fue posible leer el archivo. Intente nuevamente.");
		}
	}
}
