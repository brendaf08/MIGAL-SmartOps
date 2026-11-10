package com.migal.smartops.recepciones.model;

import com.migal.smartops.catalogo.model.Almacen;
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
 * Línea de una recepción con la cantidad recibida en una ubicación (tabla {@code detalle_recepcion}). Un producto puede
 * aparecer en varias ubicaciones, pero no dos veces en la misma.
 */
@Entity
@Table(name = "detalle_recepcion")
public class DetalleRecepcion {

	@EmbeddedId
	private DetalleRecepcionId id;

	@MapsId("idRecepcion")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_recepcion", nullable = false)
	private Recepcion recepcion;

	@MapsId("idProducto")
	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "id_producto", nullable = false)
	private Producto producto;

	@MapsId("idAlmacenDestino")
	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "id_almacen_destino", nullable = false)
	private Almacen almacen;

	@Column(name = "cantidad_recibida", nullable = false, precision = 14, scale = 3)
	private BigDecimal cantidadRecibida;

	protected DetalleRecepcion() {
		// Requerido por JPA.
	}

	DetalleRecepcion(Recepcion recepcion, Producto producto, Almacen almacen, BigDecimal cantidadRecibida) {
		this.recepcion = recepcion;
		this.producto = producto;
		this.almacen = almacen;
		this.id = new DetalleRecepcionId(recepcion.getId(), producto.getId(), almacen.getId());
		this.cantidadRecibida = cantidadRecibida.setScale(3, RoundingMode.HALF_UP);
	}

	/** Reemplaza la cantidad recibida. Puede ser cero mientras la recepción está pendiente. */
	void actualizar(BigDecimal cantidad) {
		this.cantidadRecibida = cantidad.setScale(3, RoundingMode.HALF_UP);
	}

	public DetalleRecepcionId getId() {
		return id;
	}

	public Recepcion getRecepcion() {
		return recepcion;
	}

	public Producto getProducto() {
		return producto;
	}

	public Almacen getAlmacen() {
		return almacen;
	}

	public BigDecimal getCantidadRecibida() {
		return cantidadRecibida;
	}
}
