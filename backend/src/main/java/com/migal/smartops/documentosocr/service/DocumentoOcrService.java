package com.migal.smartops.documentosocr.service;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.comun.web.RespuestaProblema.ErrorCampo;
import com.migal.smartops.documentosocr.config.PropiedadesDocumentos;
import com.migal.smartops.documentosocr.model.DocumentoDigital;
import com.migal.smartops.documentosocr.model.EstadoProcesamiento;
import com.migal.smartops.documentosocr.model.TipoDocumento;
import com.migal.smartops.documentosocr.repository.DocumentoDigitalRepository;
import com.migal.smartops.seguridad.config.UsuarioAutenticado;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.service.UsuarioService;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Documentos digitales del módulo de documentos OCR (CU-08): carga, consulta, listado y descarte. Cada operación aplica la
 * restricción por rol: el Vendedor opera documentos de tipo solicitud_cliente, el Almacenero de tipo documento_recepcion y
 * el Administrador de ambos (contrato, DocumentoTipo). La carga encola el archivo en el proveedor OCR dentro de la misma
 * transacción: si el encolado falla, no queda ningún documento registrado y el archivo se elimina.
 */
@Service
public class DocumentoOcrService {

	/** Zona horaria con la que se interpretan los rangos de fechas del listado. */
	private static final ZoneId ZONA = ZoneId.of("America/Lima");

	private final DocumentoDigitalRepository documentos;
	private final UsuarioService usuarios;
	private final ProveedorOcr proveedor;
	private final AlmacenDocumentos almacen;
	private final PropiedadesDocumentos propiedades;
	private final TransactionTemplate transaccion;
	private final Clock reloj;

	public DocumentoOcrService(DocumentoDigitalRepository documentos, UsuarioService usuarios, ProveedorOcr proveedor,
			AlmacenDocumentos almacen, PropiedadesDocumentos propiedades, PlatformTransactionManager transacciones,
			Clock reloj) {
		this.documentos = documentos;
		this.usuarios = usuarios;
		this.proveedor = proveedor;
		this.almacen = almacen;
		this.propiedades = propiedades;
		this.transaccion = new TransactionTemplate(transacciones);
		this.reloj = reloj;
	}

	/** Tipos de documento que un rol puede operar. Un rol sin tipos no opera documentos. */
	public static Set<TipoDocumento> tiposPermitidos(RolUsuario rol) {
		return switch (rol) {
			case VENDEDOR -> EnumSet.of(TipoDocumento.SOLICITUD_CLIENTE);
			case ALMACENERO -> EnumSet.of(TipoDocumento.DOCUMENTO_RECEPCION);
			case ADMINISTRADOR -> EnumSet.allOf(TipoDocumento.class);
			case REPONEDOR -> EnumSet.noneOf(TipoDocumento.class);
		};
	}

	/**
	 * Lista paginada de documentos vigentes que el rol puede operar. Los filtros de tipo que el rol no puede operar no
	 * muestran documentos. El rango de fechas es inclusivo y se interpreta en la zona de Lima.
	 */
	@Transactional(readOnly = true)
	public Page<DocumentoDigital> listar(RolUsuario rol, EstadoProcesamiento estado, TipoDocumento tipo, LocalDate desde,
			LocalDate hasta, Pageable paginacion) {
		Set<TipoDocumento> permitidos = tiposPermitidos(rol);
		Instant inicio = desde == null ? null : desde.atStartOfDay(ZONA).toInstant();
		Instant fin = hasta == null ? null : hasta.plusDays(1).atStartOfDay(ZONA).toInstant();
		Specification<DocumentoDigital> filtros = (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			condiciones.add(cb.isNull(raiz.get("fechaEliminacion")));
			if (permitidos.isEmpty()) {
				condiciones.add(cb.disjunction());
			} else {
				condiciones.add(raiz.get("tipoDocumento").in(permitidos.stream().map(TipoDocumento::valor).toList()));
			}
			if (tipo != null) {
				condiciones.add(cb.equal(raiz.get("tipoDocumento"), tipo.valor()));
			}
			if (estado != null) {
				condiciones.add(cb.equal(raiz.get("estadoProcesamiento"), estado.valor()));
			}
			if (inicio != null) {
				condiciones.add(cb.greaterThanOrEqualTo(raiz.get("fechaSubida"), inicio));
			}
			if (fin != null) {
				condiciones.add(cb.lessThan(raiz.get("fechaSubida"), fin));
			}
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
		return documentos.findAll(filtros, paginacion);
	}

	/** Consulta un documento vigente con sus líneas. Un tipo que el rol no opera responde PERMISO_INSUFICIENTE. */
	@Transactional(readOnly = true)
	public DocumentoDigital consultar(UsuarioAutenticado actor, long id) {
		DocumentoDigital documento = vigente(id);
		exigirTipoPermitido(actor, documento);
		return inicializar(documento);
	}

	/**
	 * Carga un documento y lo encola para el procesamiento OCR. Primero se verifican el rol y el archivo; luego se guarda
	 * el contenido y se registra el documento en la misma transacción que el encolado. Un fallo del encolado revierte el
	 * registro y elimina el archivo.
	 */
	public DocumentoDigital cargar(UsuarioAutenticado actor, TipoDocumento tipo, String nombreArchivo, byte[] contenido) {
		if (!tiposPermitidos(actor.rol()).contains(tipo)) {
			throw new ErrorDeNegocio(CodigoError.PERMISO_INSUFICIENTE,
					"El rol del usuario no permite cargar documentos de este tipo.");
		}
		validarArchivo(contenido);
		String extension = FormatoArchivoOcr.extensionDe(contenido).orElseThrow();
		String nombre = nombreNormalizado(nombreArchivo);
		String ruta = almacen.guardar(contenido, extension);
		try {
			return transaccion.execute(estado -> {
				Usuario responsable = usuarios.consultar(actor.id());
				DocumentoDigital documento = documentos.saveAndFlush(new DocumentoDigital(responsable, tipo, nombre, ruta,
						contenido.length, reloj.instant()));
				proveedor.encolar(new SolicitudOcr(documento.getId(), nombre, almacen.resolver(ruta), tipo));
				return inicializar(documento);
			});
		} catch (RuntimeException e) {
			almacen.eliminar(ruta);
			throw e;
		}
	}

	/** Descarta un documento mediante eliminación lógica. El registro se conserva y deja de aparecer en los listados. */
	@Transactional
	public void descartar(UsuarioAutenticado actor, long id) {
		DocumentoDigital documento = documentos.bloquearPorId(id)
				.filter(d -> !d.estaEliminado())
				.orElseThrow(DocumentoOcrService::noEncontrado);
		exigirTipoPermitido(actor, documento);
		documento.eliminar(reloj.instant());
	}

	/** Documento vigente con el identificador indicado, o RECURSO_NO_ENCONTRADO. */
	DocumentoDigital vigente(long id) {
		return documentos.findByIdAndFechaEliminacionIsNull(id).orElseThrow(DocumentoOcrService::noEncontrado);
	}

	/** Verifica que el rol del usuario pueda operar el tipo del documento. */
	void exigirTipoPermitido(UsuarioAutenticado actor, DocumentoDigital documento) {
		if (!tiposPermitidos(actor.rol()).contains(documento.getTipo())) {
			throw new ErrorDeNegocio(CodigoError.PERMISO_INSUFICIENTE,
					"El rol del usuario no permite operar documentos de este tipo.");
		}
	}

	/**
	 * Carga en memoria las asociaciones que la respuesta necesita, dentro de la transacción. Con {@code open-in-view}
	 * desactivado, las asociaciones perezosas no pueden inicializarse después del servicio.
	 */
	DocumentoDigital inicializar(DocumentoDigital documento) {
		documento.getUsuario().getUsername();
		documento.getDetalles().forEach(linea -> {
			if (linea.getProducto() != null) {
				linea.getProducto().getSku();
			}
			linea.getCoincidencias().forEach(c -> c.getProducto().getSku());
		});
		return documento;
	}

	private void validarArchivo(byte[] contenido) {
		if (contenido == null || contenido.length == 0) {
			throw validacionArchivo("vacio", "El archivo está vacío.");
		}
		if (contenido.length > propiedades.tamanoMaximoBytes()) {
			throw validacionArchivo("tamano_excedido", "El archivo supera el tamaño máximo permitido.");
		}
		if (FormatoArchivoOcr.extensionDe(contenido).isEmpty()) {
			throw validacionArchivo("formato_no_admitido", "El formato del archivo no es JPG, PNG ni PDF.");
		}
	}

	private static String nombreNormalizado(String nombre) {
		String limpio = nombre == null || nombre.isBlank() ? "documento" : nombre.strip();
		return limpio.length() > 255 ? limpio.substring(limpio.length() - 255) : limpio;
	}

	/** Error de validación del archivo, con el campo {@code archivo} en la lista de errores. */
	static ErrorDeNegocio validacionArchivo(String codigo, String mensaje) {
		return new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, mensaje,
				Map.of("errores", List.of(new ErrorCampo("archivo", codigo, mensaje))));
	}

	static ErrorDeNegocio noEncontrado() {
		return new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El documento no existe.");
	}
}
