package com.migal.smartops.documentosocr.model;

import com.migal.smartops.seguridad.model.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Documento digital cargado para el procesamiento OCR (tabla {@code documento_digital}, migraciones V9 y V13). El archivo
 * se conserva en el almacenamiento y solo se referencia por su ruta. Un documento apoya a una cotización o a una recepción,
 * nunca a ambas. La eliminación es lógica: el registro se conserva con su fecha de eliminación.
 */
@Entity
@Table(name = "documento_digital")
public class DocumentoDigital {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_documento")
	private Long id;

	@Column(name = "ruta_archivo", nullable = false, length = 500, updatable = false)
	private String rutaArchivo;

	@Column(name = "nombre_archivo", nullable = false, length = 255, updatable = false)
	private String nombreArchivo;

	@Column(name = "tamano_bytes", nullable = false, updatable = false)
	private long tamanoBytes;

	@Column(name = "fecha_subida", nullable = false, updatable = false)
	private Instant fechaSubida;

	@Column(name = "estado_procesamiento", nullable = false, length = 20)
	private String estadoProcesamiento;

	@Column(name = "tipo_documento", nullable = false, length = 50, updatable = false)
	private String tipoDocumento;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_usuario", nullable = false, updatable = false)
	private Usuario usuario;

	/** Cotización para la que el documento apoya la solicitud. Nula mientras no se asocie. */
	@Column(name = "id_cotizacion")
	private Long idCotizacion;

	/** Recepción para la que el documento sirve de apoyo. Nula mientras no se asocie. */
	@Column(name = "id_recepcion")
	private Long idRecepcion;

	/** Código del último error de procesamiento, según el catálogo de errores. Nulo cuando no hay error. */
	@Column(name = "codigo_error", length = 40)
	private String codigoError;

	@Column(name = "fecha_eliminacion")
	private Instant fechaEliminacion;

	@OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	private List<DetalleExtraido> detalles = new ArrayList<>();

	protected DocumentoDigital() {
		// Requerido por JPA.
	}

	/** Documento recién cargado: pendiente de procesamiento, sin asociación y sin error. */
	public DocumentoDigital(Usuario usuario, TipoDocumento tipo, String nombreArchivo, String rutaArchivo, long tamanoBytes,
			Instant fechaSubida) {
		this.usuario = usuario;
		this.tipoDocumento = tipo.valor();
		this.nombreArchivo = nombreArchivo;
		this.rutaArchivo = rutaArchivo;
		this.tamanoBytes = tamanoBytes;
		this.fechaSubida = fechaSubida;
		this.estadoProcesamiento = EstadoProcesamiento.PENDIENTE.valor();
	}

	public Long getId() {
		return id;
	}

	public String getRutaArchivo() {
		return rutaArchivo;
	}

	public String getNombreArchivo() {
		return nombreArchivo;
	}

	public long getTamanoBytes() {
		return tamanoBytes;
	}

	public Instant getFechaSubida() {
		return fechaSubida;
	}

	public EstadoProcesamiento getEstado() {
		return EstadoProcesamiento.de(estadoProcesamiento);
	}

	public TipoDocumento getTipo() {
		return TipoDocumento.de(tipoDocumento);
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public Long getIdCotizacion() {
		return idCotizacion;
	}

	public Long getIdRecepcion() {
		return idRecepcion;
	}

	public String getCodigoError() {
		return codigoError;
	}

	public Instant getFechaEliminacion() {
		return fechaEliminacion;
	}

	public List<DetalleExtraido> getDetalles() {
		return detalles;
	}

	/** Indica si el documento tiene una operación asociada, cotización o recepción. */
	public boolean estaAsociado() {
		return idCotizacion != null || idRecepcion != null;
	}

	/** Indica si el documento fue descartado mediante eliminación lógica. */
	public boolean estaEliminado() {
		return fechaEliminacion != null;
	}

	/** Registra las líneas extraídas y pasa el documento a procesado_ocr. Limpia el error del intento anterior. */
	public void registrarLineas(List<DetalleExtraido> lineas) {
		detalles.clear();
		lineas.forEach(this::agregarDetalle);
		this.estadoProcesamiento = EstadoProcesamiento.PROCESADO_OCR.valor();
		this.codigoError = null;
	}

	/** Agrega una línea extraída al documento. */
	public void agregarDetalle(DetalleExtraido linea) {
		detalles.add(linea);
		linea.asignarDocumento(this);
	}

	/** Registra un fallo de lectura. El documento conserva el estado pendiente para su reprocesamiento. */
	public void registrarError(String codigo) {
		this.codigoError = codigo;
	}

	/** Prepara el documento para un nuevo intento de procesamiento: quita el error registrado. */
	public void reprocesar() {
		this.codigoError = null;
	}

	/** Pasa el documento a validado. Las líneas ya están confirmadas por el usuario. */
	public void validar() {
		this.estadoProcesamiento = EstadoProcesamiento.VALIDADO.valor();
	}

	/** Asocia el documento validado con una cotización. */
	public void asociarCotizacion(long idCotizacion) {
		this.idCotizacion = idCotizacion;
	}

	/** Asocia el documento validado con una recepción. */
	public void asociarRecepcion(long idRecepcion) {
		this.idRecepcion = idRecepcion;
	}

	/** Elimina lógicamente el documento. El registro se conserva. */
	public void eliminar(Instant ahora) {
		this.fechaEliminacion = ahora;
	}
}
