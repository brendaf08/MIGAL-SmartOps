package com.migal.smartops.inventario.model;

import com.migal.smartops.catalogo.model.Almacen;
import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
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

/**
 * Existencia física de un producto en una ubicación (tabla {@code inventario}). La cantidad reservada y la libre
 * son derivadas y no se almacenan. La cantidad física solo cambia mediante {@link #sumar} y {@link #restar},
 * dentro de la transacción que registra el movimiento de Kardex.
 */
@Entity
@Table(name = "inventario")
public class Inventario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_inventario")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_producto", nullable = false, updatable = false)
	private Producto producto;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_almacen", nullable = false, updatable = false)
	private Almacen almacen;

	@Column(name = "cantidad_fisica", nullable = false, precision = 14, scale = 3)
	private BigDecimal cantidadFisica;

	protected Inventario() {
		// Requerido por JPA.
	}

	public Inventario(Producto producto, Almacen almacen, BigDecimal cantidadFisica) {
		this.producto = producto;
		this.almacen = almacen;
		this.cantidadFisica = cantidadFisica;
	}

	public Long getId() {
		return id;
	}

	public Producto getProducto() {
		return producto;
	}

	public Almacen getAlmacen() {
		return almacen;
	}

	public BigDecimal getCantidadFisica() {
		return cantidadFisica;
	}

	/** Incrementa la cantidad física. */
	public void sumar(BigDecimal cantidad) {
		exigirPositiva(cantidad);
		this.cantidadFisica = cantidadFisica.add(cantidad);
	}

	/** Reduce la cantidad física. El saldo no puede quedar negativo. */
	public void restar(BigDecimal cantidad) {
		exigirPositiva(cantidad);
		if (cantidadFisica.compareTo(cantidad) < 0) {
			throw new ErrorDeNegocio(CodigoError.STOCK_INSUFICIENTE, "La existencia física no alcanza la cantidad indicada.");
		}
		this.cantidadFisica = cantidadFisica.subtract(cantidad);
	}

	private static void exigirPositiva(BigDecimal cantidad) {
		if (cantidad == null || cantidad.signum() <= 0) {
			throw new IllegalArgumentException("La cantidad debe ser positiva.");
		}
	}
}
