package com.migal.smartops.comercial.model;

import com.migal.smartops.catalogo.model.Producto;
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
import java.math.RoundingMode;

/**
 * Línea de una venta (tabla {@code detalle_venta}). La cantidad vendida es la cantidad comprometida comercialmente;
 * la cobertura se obtiene de las reservas activas y no se almacena. El subtotal es derivado.
 */
@Entity
@Table(name = "detalle_venta")
public class DetalleVenta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_detalle_venta")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_venta", nullable = false, updatable = false)
	private Venta venta;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "id_producto", nullable = false, updatable = false)
	private Producto producto;

	@Column(name = "cantidad_vendida", nullable = false, precision = 14, scale = 3)
	private BigDecimal cantidadVendida;

	@Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
	private BigDecimal precioUnitario;

	protected DetalleVenta() {
		// Requerido por JPA.
	}

	/** Crea una línea de venta. La cantidad y el precio se redondean a la escala de la columna. */
	DetalleVenta(Venta venta, Producto producto, BigDecimal cantidad, BigDecimal precioUnitario) {
		this.venta = venta;
		this.producto = producto;
		this.cantidadVendida = cantidad.setScale(3, RoundingMode.HALF_UP);
		this.precioUnitario = precioUnitario.setScale(2, RoundingMode.HALF_UP);
	}

	/** Subtotal derivado de la línea: cantidad vendida por precio unitario, redondeado a dos decimales. */
	public BigDecimal subtotal() {
		return cantidadVendida.multiply(precioUnitario).setScale(2, RoundingMode.HALF_UP);
	}

	public Long getId() {
		return id;
	}

	public Venta getVenta() {
		return venta;
	}

	public Producto getProducto() {
		return producto;
	}

	public BigDecimal getCantidadVendida() {
		return cantidadVendida;
	}

	public BigDecimal getPrecioUnitario() {
		return precioUnitario;
	}
}
