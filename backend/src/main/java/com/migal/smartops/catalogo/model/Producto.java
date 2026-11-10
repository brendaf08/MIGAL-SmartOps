package com.migal.smartops.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.Generated;

/**
 * Producto del catálogo maestro (tabla {@code producto}). Su clave natural es el SKU, único en el catálogo y
 * no modificable después del registro. La desactivación es lógica mediante {@code estadoActivo}.
 */
@Entity
@Table(name = "producto")
public class Producto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_producto")
	private Long id;

	@Column(name = "sku", nullable = false, length = 50, updatable = false)
	private String sku;

	@Column(name = "nombre_oficial", nullable = false, length = 200)
	private String nombreOficial;

	@Column(name = "descripcion")
	private String descripcion;

	@Column(name = "unidad_medida", nullable = false, length = 30)
	private String unidadMedida;

	@Column(name = "precio_referencial", nullable = false, precision = 14, scale = 2)
	private BigDecimal precioReferencial;

	@Column(name = "stock_minimo", nullable = false, precision = 14, scale = 3)
	private BigDecimal stockMinimo;

	@Column(name = "estado_activo", nullable = false)
	private boolean estadoActivo = true;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "id_categoria", nullable = false)
	private Categoria categoria;

	@Column(name = "marca", length = 100)
	private String marca;

	@Column(name = "presentacion", length = 100)
	private String presentacion;

	@Generated
	@Column(name = "fecha_registro", insertable = false, updatable = false)
	private Instant fechaRegistro;

	@Column(name = "fecha_actualizacion")
	private Instant fechaActualizacion;

	protected Producto() {
		// Requerido por JPA.
	}

	/** Crea un producto sin existencias, con la vigencia indicada. La fecha de actualización queda nula. */
	public Producto(String sku, String nombreOficial, String descripcion, String unidadMedida,
			BigDecimal precioReferencial, BigDecimal stockMinimo, Categoria categoria, String marca,
			String presentacion, boolean estadoActivo) {
		this.sku = sku;
		this.nombreOficial = nombreOficial;
		this.descripcion = descripcion;
		this.unidadMedida = unidadMedida;
		this.precioReferencial = precioReferencial;
		this.stockMinimo = stockMinimo;
		this.categoria = categoria;
		this.marca = marca;
		this.presentacion = presentacion;
		this.estadoActivo = estadoActivo;
	}

	public Long getId() {
		return id;
	}

	public String getSku() {
		return sku;
	}

	public String getNombreOficial() {
		return nombreOficial;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public String getUnidadMedida() {
		return unidadMedida;
	}

	public BigDecimal getPrecioReferencial() {
		return precioReferencial;
	}

	public BigDecimal getStockMinimo() {
		return stockMinimo;
	}

	public boolean isEstadoActivo() {
		return estadoActivo;
	}

	public Categoria getCategoria() {
		return categoria;
	}

	public String getMarca() {
		return marca;
	}

	public String getPresentacion() {
		return presentacion;
	}

	public Instant getFechaRegistro() {
		return fechaRegistro;
	}

	public Instant getFechaActualizacion() {
		return fechaActualizacion;
	}

	/** Actualiza los datos del producto, sin modificar el SKU, y registra el momento de la modificación. */
	public void actualizar(String nombreOficial, String descripcion, String unidadMedida, BigDecimal precioReferencial,
			BigDecimal stockMinimo, Categoria categoria, String marca, String presentacion, Instant ahora) {
		this.nombreOficial = nombreOficial;
		this.descripcion = descripcion;
		this.unidadMedida = unidadMedida;
		this.precioReferencial = precioReferencial;
		this.stockMinimo = stockMinimo;
		this.categoria = categoria;
		this.marca = marca;
		this.presentacion = presentacion;
		this.fechaActualizacion = ahora;
	}

	/** Activa o desactiva la vigencia del producto y registra el momento de la modificación. */
	public void cambiarEstado(boolean activo, Instant ahora) {
		this.estadoActivo = activo;
		this.fechaActualizacion = ahora;
	}
}
