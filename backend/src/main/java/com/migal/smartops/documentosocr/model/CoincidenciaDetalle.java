package com.migal.smartops.documentosocr.model;

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

/**
 * Producto del catálogo propuesto para una línea extraída, con su similitud y su posición en la lista de sugerencias
 * (tabla {@code coincidencia_detalle}, migración V13). Un producto aparece una sola vez por línea.
 */
@Entity
@Table(name = "coincidencia_detalle")
public class CoincidenciaDetalle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_coincidencia")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_detalle_extraido", nullable = false, updatable = false)
	private DetalleExtraido detalle;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_producto", nullable = false, updatable = false)
	private Producto producto;

	@Column(name = "similitud", nullable = false, precision = 4, scale = 3, updatable = false)
	private BigDecimal similitud;

	@Column(name = "posicion", nullable = false, updatable = false)
	private short posicion;

	protected CoincidenciaDetalle() {
		// Requerido por JPA.
	}

	/** Coincidencia en la posición indicada, que comienza en uno. */
	public CoincidenciaDetalle(Producto producto, BigDecimal similitud, short posicion) {
		this.producto = producto;
		this.similitud = similitud;
		this.posicion = posicion;
	}

	void asignarDetalle(DetalleExtraido detalle) {
		this.detalle = detalle;
	}

	public Long getId() {
		return id;
	}

	public Producto getProducto() {
		return producto;
	}

	public BigDecimal getSimilitud() {
		return similitud;
	}

	public short getPosicion() {
		return posicion;
	}
}
