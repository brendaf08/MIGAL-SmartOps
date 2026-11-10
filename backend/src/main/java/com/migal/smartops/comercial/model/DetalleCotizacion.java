package com.migal.smartops.comercial.model;

import com.migal.smartops.catalogo.model.Producto;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Línea de una cotización (tabla {@code detalle_cotizacion}). Un producto aparece una sola vez en cada cotización.
 * El precio unitario puede diferir del precio referencial del producto. El subtotal es derivado y no se almacena.
 */
@Entity
@Table(name = "detalle_cotizacion")
public class DetalleCotizacion {

	@EmbeddedId
	private DetalleCotizacionId id;

	@MapsId("idCotizacion")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_cotizacion", nullable = false)
	private Cotizacion cotizacion;

	@MapsId("idProducto")
	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "id_producto", nullable = false)
	private Producto producto;

	@Column(name = "cantidad_cotizada", nullable = false, precision = 14, scale = 3)
	private BigDecimal cantidadCotizada;

	@Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
	private BigDecimal precioUnitario;

	protected DetalleCotizacion() {
		// Requerido por JPA.
	}

	DetalleCotizacion(Cotizacion cotizacion, Producto producto, BigDecimal cantidad, BigDecimal precioUnitario) {
		this.cotizacion = cotizacion;
		this.producto = producto;
		this.id = new DetalleCotizacionId(null, producto.getId());
		this.cantidadCotizada = cantidad.setScale(3, RoundingMode.HALF_UP);
		this.precioUnitario = precioUnitario.setScale(2, RoundingMode.HALF_UP);
	}

	/** Reemplaza la cantidad y el precio unitario de la línea. La cantidad debe ser mayor que cero. */
	public void actualizar(BigDecimal cantidad, BigDecimal precioUnitario) {
		this.cantidadCotizada = cantidad.setScale(3, RoundingMode.HALF_UP);
		this.precioUnitario = precioUnitario.setScale(2, RoundingMode.HALF_UP);
	}

	/** Subtotal derivado de la línea: cantidad por precio unitario, redondeado a dos decimales. */
	public BigDecimal subtotal() {
		return cantidadCotizada.multiply(precioUnitario).setScale(2, RoundingMode.HALF_UP);
	}

	public DetalleCotizacionId getId() {
		return id;
	}

	public Cotizacion getCotizacion() {
		return cotizacion;
	}

	public Producto getProducto() {
		return producto;
	}

	public BigDecimal getCantidadCotizada() {
		return cantidadCotizada;
	}

	public BigDecimal getPrecioUnitario() {
		return precioUnitario;
	}
}
