package com.migal.smartops.documentosocr.model;

import com.migal.smartops.catalogo.model.Producto;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Línea extraída por el OCR de un documento (tabla {@code detalle_extraido}, migraciones V9 y V13). El texto crudo y la
 * cantidad leída son la lectura original; el texto corregido, la cantidad corregida y el producto confirmado los fija el
 * usuario. El producto es nulo hasta su confirmación. Una línea descartada se conserva y se excluye de la validación.
 */
@Entity
@Table(name = "detalle_extraido")
public class DetalleExtraido {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_detalle_extraido")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_documento", nullable = false, updatable = false)
	private DocumentoDigital documento;

	@Column(name = "texto_crudo", nullable = false)
	private String textoCrudo;

	@Column(name = "texto_corregido")
	private String textoCorregido;

	@Column(name = "cantidad_leida", precision = 14, scale = 3)
	private BigDecimal cantidadLeida;

	@Column(name = "cantidad_corregida", precision = 14, scale = 3)
	private BigDecimal cantidadCorregida;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_producto")
	private Producto producto;

	/** SKU de la mejor coincidencia propuesta por el Fuzzy Matching. No implica confirmación. */
	@Column(name = "sku_sugerido", length = 50)
	private String skuSugerido;

	/** Confianza de la mejor coincidencia propuesta, de 0 a 1. */
	@Column(name = "confianza", precision = 4, scale = 3)
	private BigDecimal confianza;

	@Column(name = "requiere_revision", nullable = false)
	private boolean requiereRevision;

	@Column(name = "descartado", nullable = false)
	private boolean descartado;

	@OneToMany(mappedBy = "detalle", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("posicion ASC")
	private List<CoincidenciaDetalle> coincidencias = new ArrayList<>();

	protected DetalleExtraido() {
		// Requerido por JPA.
	}

	/** Línea recién extraída, sin producto confirmado ni corrección, con la lectura original del OCR. */
	public DetalleExtraido(String textoCrudo, BigDecimal cantidadLeida) {
		this.textoCrudo = textoCrudo;
		this.cantidadLeida = cantidadLeida;
		this.requiereRevision = true;
		this.descartado = false;
	}

	void asignarDocumento(DocumentoDigital documento) {
		this.documento = documento;
	}

	public Long getId() {
		return id;
	}

	public DocumentoDigital getDocumento() {
		return documento;
	}

	public String getTextoCrudo() {
		return textoCrudo;
	}

	public String getTextoCorregido() {
		return textoCorregido;
	}

	public BigDecimal getCantidadLeida() {
		return cantidadLeida;
	}

	public BigDecimal getCantidadCorregida() {
		return cantidadCorregida;
	}

	public Producto getProducto() {
		return producto;
	}

	public String getSkuSugerido() {
		return skuSugerido;
	}

	public BigDecimal getConfianza() {
		return confianza;
	}

	public boolean isRequiereRevision() {
		return requiereRevision;
	}

	public boolean isDescartado() {
		return descartado;
	}

	public List<CoincidenciaDetalle> getCoincidencias() {
		return coincidencias;
	}

	/** Reemplaza las coincidencias propuestas y recalcula la sugerencia y la necesidad de revisión. */
	public void proponerCoincidencias(List<CoincidenciaDetalle> propuestas, BigDecimal umbralAlta) {
		coincidencias.clear();
		propuestas.forEach(c -> c.asignarDetalle(this));
		coincidencias.addAll(propuestas);
		CoincidenciaDetalle mejor = propuestas.isEmpty() ? null : propuestas.get(0);
		this.skuSugerido = mejor == null ? null : mejor.getProducto().getSku();
		this.confianza = mejor == null ? null : mejor.getSimilitud();
		recalcularRevision(umbralAlta);
	}

	/** Corrige el texto. Un valor nulo elimina la corrección previa. */
	public void corregirTexto(String texto) {
		this.textoCorregido = texto;
	}

	/** Confirma el producto de la línea. Un valor nulo desasocia el producto. */
	public void confirmarProducto(Producto producto, BigDecimal umbralAlta) {
		this.producto = producto;
		recalcularRevision(umbralAlta);
	}

	/** Fija la cantidad corregida. Un valor nulo elimina la cantidad informada. */
	public void corregirCantidad(BigDecimal cantidad) {
		this.cantidadCorregida = cantidad;
	}

	/** Marca la línea como descartada. Una línea descartada no participa en la validación ni en la precarga. */
	public void descartar() {
		this.descartado = true;
	}

	/**
	 * Recalcula si la línea necesita revisión: sin producto confirmado, o con una mejor coincidencia por debajo del
	 * umbral alto (contrato, DocumentoDetalle.requiereRevision).
	 */
	private void recalcularRevision(BigDecimal umbralAlta) {
		boolean confirmadaConfianzaAlta = producto != null && confianza != null && confianza.compareTo(umbralAlta) >= 0;
		this.requiereRevision = !confirmadaConfianzaAlta;
	}
}
