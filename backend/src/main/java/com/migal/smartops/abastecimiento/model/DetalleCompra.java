package com.migal.smartops.abastecimiento.model;

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
 * Línea de una orden de compra (tabla {@code detalle_compra}). Un producto aparece una sola vez en cada orden. La
 * cantidad recibida y la pendiente de recepción son derivadas y no se almacenan; el subtotal también es derivado.
 */
@Entity
@Table(name = "detalle_compra")
public class DetalleCompra {

	@EmbeddedId
	private DetalleCompraId id;

	@MapsId("idOrden")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_orden", nullable = false)
	private OrdenCompra orden;

	@MapsId("idProducto")
	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "id_producto", nullable = false)
	private Producto producto;

	@Column(name = "cantidad_comprada", nullable = false, precision = 14, scale = 3)
	private BigDecimal cantidadComprada;

	@Column(name = "costo_unitario", nullable = false, precision = 14, scale = 2)
	private BigDecimal costoUnitario;

	protected DetalleCompra() {
		// Requerido por JPA.
	}

	DetalleCompra(OrdenCompra orden, Producto producto, BigDecimal cantidadComprada, BigDecimal costoUnitario) {
		this.orden = orden;
		this.producto = producto;
		this.id = new DetalleCompraId(orden.getId(), producto.getId());
		this.cantidadComprada = cantidadComprada.setScale(3, RoundingMode.HALF_UP);
		this.costoUnitario = costoUnitario.setScale(2, RoundingMode.HALF_UP);
	}

	/** Reemplaza la cantidad y el costo unitario de la línea. */
	void actualizar(BigDecimal cantidad, BigDecimal costo) {
		this.cantidadComprada = cantidad.setScale(3, RoundingMode.HALF_UP);
		this.costoUnitario = costo.setScale(2, RoundingMode.HALF_UP);
	}

	/** Subtotal derivado de la línea: cantidad por costo unitario, redondeado a dos decimales. */
	public BigDecimal subtotal() {
		return cantidadComprada.multiply(costoUnitario).setScale(2, RoundingMode.HALF_UP);
	}

	public DetalleCompraId getId() {
		return id;
	}

	public OrdenCompra getOrden() {
		return orden;
	}

	public Producto getProducto() {
		return producto;
	}

	public BigDecimal getCantidadComprada() {
		return cantidadComprada;
	}

	public BigDecimal getCostoUnitario() {
		return costoUnitario;
	}
}
