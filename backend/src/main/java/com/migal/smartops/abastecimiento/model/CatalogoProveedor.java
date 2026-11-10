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
import java.time.Instant;

/**
 * Producto que suministra un proveedor, con su precio de compra y su plazo de entrega referencial (tabla
 * {@code catalogo_proveedor}). La fecha de actualización del precio solo cambia cuando el precio cambia.
 */
@Entity
@Table(name = "catalogo_proveedor")
public class CatalogoProveedor {

	@EmbeddedId
	private CatalogoProveedorId id;

	@MapsId("idProveedor")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_proveedor", nullable = false)
	private Proveedor proveedor;

	@MapsId("idProducto")
	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "id_producto", nullable = false)
	private Producto producto;

	@Column(name = "precio_compra", nullable = false, precision = 14, scale = 2)
	private BigDecimal precioCompra;

	@Column(name = "plazo_entrega_referencial_dias")
	private Integer plazoEntregaReferencialDias;

	@Column(name = "fecha_actualizacion_precio")
	private Instant fechaActualizacionPrecio;

	protected CatalogoProveedor() {
		// Requerido por JPA.
	}

	public CatalogoProveedor(Proveedor proveedor, Producto producto, BigDecimal precioCompra,
			Integer plazoEntregaReferencialDias, Instant ahora) {
		this.proveedor = proveedor;
		this.producto = producto;
		this.id = new CatalogoProveedorId(proveedor.getId(), producto.getId());
		this.precioCompra = precioCompra.setScale(2, RoundingMode.HALF_UP);
		this.plazoEntregaReferencialDias = plazoEntregaReferencialDias;
		this.fechaActualizacionPrecio = ahora;
	}

	/** Actualiza el precio y el plazo. Si el precio cambia, registra la marca de la actualización. */
	public void actualizar(BigDecimal nuevoPrecio, Integer nuevoPlazo, Instant ahora) {
		BigDecimal precio = nuevoPrecio.setScale(2, RoundingMode.HALF_UP);
		if (precio.compareTo(precioCompra) != 0) {
			this.fechaActualizacionPrecio = ahora;
		}
		this.precioCompra = precio;
		this.plazoEntregaReferencialDias = nuevoPlazo;
	}

	public CatalogoProveedorId getId() {
		return id;
	}

	public Proveedor getProveedor() {
		return proveedor;
	}

	public Producto getProducto() {
		return producto;
	}

	public BigDecimal getPrecioCompra() {
		return precioCompra;
	}

	public Integer getPlazoEntregaReferencialDias() {
		return plazoEntregaReferencialDias;
	}

	public Instant getFechaActualizacionPrecio() {
		return fechaActualizacionPrecio;
	}
}
