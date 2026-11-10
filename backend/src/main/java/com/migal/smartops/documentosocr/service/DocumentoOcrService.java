package com.migal.smartops.documentosocr.service;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.repository.ProductoRepository;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.comun.web.RespuestaProblema.ErrorCampo;
import com.migal.smartops.documentosocr.config.PropiedadesDocumentos;
import com.migal.smartops.documentosocr.config.PropiedadesOcr;
import com.migal.smartops.documentosocr.model.CoincidenciaDetalle;
import com.migal.smartops.documentosocr.model.DetalleExtraido;
import com.migal.smartops.documentosocr.model.DocumentoDigital;
import com.migal.smartops.documentosocr.model.EstadoProcesamiento;
import com.migal.smartops.documentosocr.model.TipoDocumento;
import com.migal.smartops.documentosocr.repository.DocumentoDigitalRepository;
import com.migal.smartops.seguridad.config.UsuarioAutenticado;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.service.UsuarioService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.simple.JdbcClient;
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

	/** Resultado del OCR para un documento pendiente: procesado con líneas, o ilegible. */
	public record ResultadoOcr(boolean ilegible, List<LineaOcr> lineas) {
	}

	/** Línea extraída por el OCR, con sus coincidencias sugeridas por SKU. */
	public record LineaOcr(String textoCrudo, BigDecimal cantidadLeida, List<CoincidenciaOcr> coincidencias) {
	}

	/** Coincidencia sugerida por el OCR: SKU del catálogo y confianza de 0 a 1. */
	public record CoincidenciaOcr(String sku, BigDecimal confianza) {
	}

	/**
	 * Corrección de una línea. Cada campo indica si la solicitud lo informó ({@code conTexto}, {@code conProducto},
	 * {@code conCantidad}), de modo que un valor nulo informado elimina el dato y uno omitido lo conserva.
	 */
	public record CorreccionDetalle(boolean conTexto, String textoCorregido, boolean conProducto, Long idProducto,
			boolean conCantidad, BigDecimal cantidad) {
	}

	/** Línea de un documento validado, lista para precargar una recepción. */
	public record LineaPrecargada(long productoId, BigDecimal cantidad) {
	}

	private static final int LONGITUD_TEXTO = 500;
	private static final BigDecimal UNO = BigDecimal.ONE;

	private final DocumentoDigitalRepository documentos;
	private final UsuarioService usuarios;
	private final ProductoRepository productos;
	private final ProveedorOcr proveedor;
	private final AlmacenDocumentos almacen;
	private final PropiedadesDocumentos propiedades;
	private final PropiedadesOcr propiedadesOcr;
	private final JdbcClient jdbc;
	private final TransactionTemplate transaccion;
	private final Clock reloj;

	public DocumentoOcrService(DocumentoDigitalRepository documentos, UsuarioService usuarios, ProductoRepository productos,
			ProveedorOcr proveedor, AlmacenDocumentos almacen, PropiedadesDocumentos propiedades,
			PropiedadesOcr propiedadesOcr, JdbcClient jdbc, PlatformTransactionManager transacciones, Clock reloj) {
		this.documentos = documentos;
		this.usuarios = usuarios;
		this.productos = productos;
		this.proveedor = proveedor;
		this.almacen = almacen;
		this.propiedades = propiedades;
		this.propiedadesOcr = propiedadesOcr;
		this.jdbc = jdbc;
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

	/**
	 * Registra el resultado del OCR para un documento pendiente. Un resultado procesado registra las líneas, sus
	 * coincidencias con el catálogo y pasa el documento a procesado_ocr. Un resultado ilegible registra DOCUMENTO_ILEGIBLE y
	 * conserva el estado pendiente. Las líneas se validan antes de registrarse: un error deja el documento sin cambios.
	 */
	@Transactional
	public DocumentoDigital registrarResultados(long id, ResultadoOcr resultado) {
		DocumentoDigital documento = bloquearVigente(id);
		if (documento.getEstado() != EstadoProcesamiento.PENDIENTE) {
			throw new ErrorDeNegocio(CodigoError.TRANSICION_NO_PERMITIDA, "El documento no está pendiente de procesamiento.");
		}
		if (resultado.ilegible()) {
			if (!resultado.lineas().isEmpty()) {
				throw validacion("Un resultado ilegible no puede incluir líneas extraídas.");
			}
			documento.registrarError(CodigoError.DOCUMENTO_ILEGIBLE.name());
			return inicializar(documento);
		}
		List<DetalleExtraido> lineas = new ArrayList<>();
		for (LineaOcr linea : resultado.lineas()) {
			lineas.add(nuevaLinea(linea));
		}
		documento.registrarLineas(lineas);
		return inicializar(documento);
	}

	/**
	 * Vuelve a encolar un documento cuyo último intento fue ilegible. Si el encolado falla, la transacción revierte la
	 * limpieza del error. Responde TRANSICION_NO_PERMITIDA si el documento no está pendiente o no tiene error registrado.
	 */
	@Transactional
	public DocumentoDigital reprocesar(UsuarioAutenticado actor, long id) {
		DocumentoDigital documento = bloquearVigente(id);
		exigirTipoPermitido(actor, documento);
		if (documento.getEstado() != EstadoProcesamiento.PENDIENTE || documento.getCodigoError() == null) {
			throw new ErrorDeNegocio(CodigoError.TRANSICION_NO_PERMITIDA,
					"Solo se reprocesan documentos pendientes con un error registrado.");
		}
		documento.reprocesar();
		proveedor.encolar(new SolicitudOcr(id, documento.getNombreArchivo(), almacen.resolver(documento.getRutaArchivo()),
				documento.getTipo()));
		return inicializar(documento);
	}

	/**
	 * Corrige el texto, el producto o la cantidad de una línea de un documento procesado. El producto debe existir y estar
	 * activo; la cantidad corregida es positiva.
	 */
	@Transactional
	public DetalleExtraido corregirDetalle(UsuarioAutenticado actor, long idDocumento, long idDetalle,
			CorreccionDetalle correccion) {
		DocumentoDigital documento = bloquearVigente(idDocumento);
		exigirTipoPermitido(actor, documento);
		DetalleExtraido linea = lineaDe(documento, idDetalle);
		exigirProcesado(documento, linea);
		BigDecimal umbralAlta = propiedadesOcr.umbrales().alta();
		if (correccion.conTexto()) {
			linea.corregirTexto(textoCorregido(correccion.textoCorregido()));
		}
		if (correccion.conProducto()) {
			linea.confirmarProducto(correccion.idProducto() == null ? null : productoActivo(correccion.idProducto()),
					umbralAlta);
		}
		if (correccion.conCantidad()) {
			linea.corregirCantidad(cantidadCorregida(correccion.cantidad()));
		}
		documentos.saveAndFlush(documento);
		inicializar(documento);
		return linea;
	}

	/** Descarta una línea extraída. La línea se conserva y se excluye de la validación y de la precarga. */
	@Transactional
	public DocumentoDigital descartarDetalle(UsuarioAutenticado actor, long idDocumento, long idDetalle) {
		DocumentoDigital documento = bloquearVigente(idDocumento);
		exigirTipoPermitido(actor, documento);
		DetalleExtraido linea = lineaDe(documento, idDetalle);
		exigirProcesado(documento, linea);
		linea.descartar();
		return inicializar(documento);
	}

	/**
	 * Confirma las líneas del documento. Cada línea no descartada debe tener producto confirmado; si alguna no lo tiene, la
	 * validación falla con la lista de errores y el documento no cambia.
	 */
	@Transactional
	public DocumentoDigital validar(UsuarioAutenticado actor, long id) {
		DocumentoDigital documento = bloquearVigente(id);
		exigirTipoPermitido(actor, documento);
		if (documento.getEstado() != EstadoProcesamiento.PROCESADO_OCR) {
			throw new ErrorDeNegocio(CodigoError.DOCUMENTO_NO_VALIDABLE,
					"Solo se validan documentos procesados por el OCR.");
		}
		List<ErrorCampo> errores = new ArrayList<>();
		long vigentes = 0;
		for (DetalleExtraido linea : documento.getDetalles()) {
			if (linea.isDescartado()) {
				continue;
			}
			vigentes++;
			if (linea.getProducto() == null) {
				errores.add(new ErrorCampo("detalles/" + linea.getId(), "producto_requerido",
						"La línea no tiene producto confirmado."));
			}
		}
		if (vigentes == 0) {
			throw validacion("El documento no tiene líneas vigentes para validar.");
		}
		if (!errores.isEmpty()) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "Hay líneas sin producto confirmado.",
					Map.of("errores", errores));
		}
		documento.validar();
		return inicializar(documento);
	}

	/**
	 * Asocia un documento validado con una cotización o con una recepción. La relación es exclusiva: se informa una de las
	 * dos operaciones y el tipo del documento debe corresponder a ella.
	 */
	@Transactional
	public DocumentoDigital asociar(UsuarioAutenticado actor, long id, Long idCotizacion, Long idRecepcion) {
		if ((idCotizacion == null) == (idRecepcion == null)) {
			throw validacion("Debe informarse una cotización o una recepción, y solo una.");
		}
		DocumentoDigital documento = bloquearVigente(id);
		exigirTipoPermitido(actor, documento);
		if (documento.getEstado() != EstadoProcesamiento.VALIDADO) {
			throw new ErrorDeNegocio(CodigoError.TRANSICION_NO_PERMITIDA, "Solo se asocian documentos validados.");
		}
		if (documento.estaAsociado()) {
			throw new ErrorDeNegocio(CodigoError.TRANSICION_NO_PERMITIDA, "El documento ya está asociado a otra operación.");
		}
		if (idCotizacion != null) {
			if (documento.getTipo() != TipoDocumento.SOLICITUD_CLIENTE || !existe("cotizacion", "id_cotizacion", idCotizacion)) {
				throw validacion("La cotización no existe o no corresponde al tipo del documento.");
			}
			documento.asociarCotizacion(idCotizacion);
		} else {
			if (documento.getTipo() != TipoDocumento.DOCUMENTO_RECEPCION
					|| !existe("orden_recepcion", "id_recepcion", idRecepcion)) {
				throw validacion("La recepción no existe o no corresponde al tipo del documento.");
			}
			documento.asociarRecepcion(idRecepcion);
		}
		return inicializar(documento);
	}

	/**
	 * Líneas de un documento validado para precargar una recepción: productos confirmados y cantidad corregida, o leída si
	 * no hay corrección, sin líneas descartadas. Lo invoca el módulo de recepciones dentro de su transacción.
	 */
	@Transactional
	public List<LineaPrecargada> precargaRecepcion(long idDocumento) {
		DocumentoDigital documento = documentos.findById(idDocumento)
				.orElseThrow(() -> new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El documento no existe."));
		boolean utilizable = documento.getEstado() == EstadoProcesamiento.VALIDADO && !documento.estaAsociado()
				&& !documento.estaEliminado();
		if (!utilizable) {
			throw new ErrorDeNegocio(CodigoError.DOCUMENTO_NO_VALIDADO,
					"El documento no está validado o ya apoya otra operación.");
		}
		List<LineaPrecargada> lineas = new ArrayList<>();
		for (DetalleExtraido linea : documento.getDetalles()) {
			BigDecimal cantidad = linea.getCantidadCorregida() != null ? linea.getCantidadCorregida() : linea.getCantidadLeida();
			if (linea.isDescartado()) {
				continue;
			}
			if (linea.getProducto() == null || cantidad == null) {
				throw validacion("El documento contiene elementos sin producto confirmado o sin cantidad.");
			}
			lineas.add(new LineaPrecargada(linea.getProducto().getId(), cantidad));
		}
		return lineas;
	}

	/** Registra el documento como apoyo de una recepción. Lo invoca el módulo de recepciones dentro de su transacción. */
	@Transactional
	public void vincularRecepcion(long idDocumento, long idRecepcion) {
		DocumentoDigital documento = documentos.findById(idDocumento).orElseThrow(DocumentoOcrService::noEncontrado);
		documento.asociarRecepcion(idRecepcion);
		documentos.saveAndFlush(documento);
	}

	/** Identificador del documento que apoya una recepción, si existe. */
	@Transactional(readOnly = true)
	public Optional<Long> documentoDeRecepcion(long idRecepcion) {
		return documentos.idDocumentoDeRecepcion(idRecepcion);
	}

	/** Documento vigente bloqueado hasta el fin de la transacción. */
	private DocumentoDigital bloquearVigente(long id) {
		return documentos.bloquearPorId(id).filter(d -> !d.estaEliminado()).orElseThrow(DocumentoOcrService::noEncontrado);
	}

	/**
	 * Construye una línea desde el resultado del OCR. Las coincidencias con confianza inferior al umbral bajo, o cuyo SKU no
	 * corresponde a un producto activo, no se proponen.
	 */
	private DetalleExtraido nuevaLinea(LineaOcr linea) {
		String texto = linea.textoCrudo() == null ? "" : linea.textoCrudo().strip();
		if (texto.isEmpty() || texto.length() > LONGITUD_TEXTO) {
			throw validacion("El texto de una línea extraída debe tener entre 1 y 500 caracteres.");
		}
		if (linea.cantidadLeida() != null && linea.cantidadLeida().signum() < 0) {
			throw validacion("La cantidad leída no puede ser negativa.");
		}
		BigDecimal umbralBaja = propiedadesOcr.umbrales().baja();
		List<Propuesta> propuestas = new ArrayList<>();
		for (CoincidenciaOcr coincidencia : linea.coincidencias()) {
			if (coincidencia.confianza() == null || coincidencia.confianza().compareTo(BigDecimal.ZERO) < 0
					|| coincidencia.confianza().compareTo(UNO) > 0) {
				throw validacion("La confianza de una coincidencia debe estar entre 0 y 1.");
			}
			BigDecimal confianza = coincidencia.confianza().setScale(3, RoundingMode.HALF_UP);
			if (confianza.compareTo(umbralBaja) < 0) {
				continue;
			}
			Optional<Producto> producto = productos.findBySku(coincidencia.sku()).filter(Producto::isEstadoActivo);
			producto.ifPresent(p -> propuestas.add(new Propuesta(p, confianza)));
		}
		propuestas.sort(Comparator.comparing(Propuesta::confianza).reversed());
		List<CoincidenciaDetalle> coincidencias = new ArrayList<>();
		for (Propuesta propuesta : propuestas) {
			boolean repetido = coincidencias.stream().anyMatch(c -> c.getProducto().getId().equals(propuesta.producto().getId()));
			if (!repetido) {
				coincidencias.add(new CoincidenciaDetalle(propuesta.producto(), propuesta.confianza(),
						(short) (coincidencias.size() + 1)));
			}
		}
		DetalleExtraido detalle = new DetalleExtraido(texto, linea.cantidadLeida());
		detalle.proponerCoincidencias(coincidencias, propiedadesOcr.umbrales().alta());
		return detalle;
	}

	/** Coincidencia resuelta contra el catálogo, antes de asignarle su posición. */
	private record Propuesta(Producto producto, BigDecimal confianza) {
	}

	private DetalleExtraido lineaDe(DocumentoDigital documento, long idDetalle) {
		return documento.getDetalles().stream().filter(l -> l.getId() == idDetalle).findFirst()
				.orElseThrow(() -> new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La línea no existe."));
	}

	/** Solo se modifican las líneas no descartadas de un documento procesado por el OCR. */
	private static void exigirProcesado(DocumentoDigital documento, DetalleExtraido linea) {
		if (documento.getEstado() != EstadoProcesamiento.PROCESADO_OCR) {
			throw new ErrorDeNegocio(CodigoError.TRANSICION_NO_PERMITIDA, "Solo se modifican líneas de documentos procesados.");
		}
		if (linea.isDescartado()) {
			throw new ErrorDeNegocio(CodigoError.TRANSICION_NO_PERMITIDA, "La línea está descartada.");
		}
	}

	private String textoCorregido(String texto) {
		if (texto == null) {
			return null;
		}
		String limpio = texto.strip();
		if (limpio.isEmpty() || limpio.length() > LONGITUD_TEXTO) {
			throw validacion("El texto corregido debe tener entre 1 y 500 caracteres.");
		}
		return limpio;
	}

	private Producto productoActivo(long idProducto) {
		Producto producto = productos.findById(idProducto).orElseThrow(() -> validacion("El producto no existe."));
		if (!producto.isEstadoActivo()) {
			throw new ErrorDeNegocio(CodigoError.PRODUCTO_INACTIVO, "El producto está inactivo.");
		}
		return producto;
	}

	private static BigDecimal cantidadCorregida(BigDecimal cantidad) {
		if (cantidad == null) {
			return null;
		}
		if (cantidad.signum() <= 0 || cantidad.stripTrailingZeros().scale() > 3) {
			throw validacion("La cantidad corregida debe ser mayor que cero, con hasta tres decimales.");
		}
		return cantidad;
	}

	private boolean existe(String tabla, String columna, long id) {
		return jdbc.sql("SELECT count(*) FROM " + tabla + " WHERE " + columna + " = :id").param("id", id)
				.query(Long.class).single() > 0;
	}

	private static ErrorDeNegocio validacion(String mensaje) {
		return new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, mensaje);
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
