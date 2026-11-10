package com.migal.smartops.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Ubicación física donde se mantienen existencias (tabla {@code almacen}): la tienda principal o un almacén.
 * La desactivación es lógica mediante {@code estadoActivo} y conserva el registro.
 */
@Entity
@Table(name = "almacen")
public class Almacen {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_almacen")
	private Long id;

	@Column(name = "nombre", nullable = false, length = 100)
	private String nombre;

	@Column(name = "tipo_ubicacion", nullable = false, length = 30)
	private String tipoUbicacion;

	@Column(name = "direccion_referencia", length = 255)
	private String direccionReferencia;

	@Column(name = "estado_activo", nullable = false)
	private boolean estadoActivo = true;

	protected Almacen() {
		// Requerido por JPA.
	}

	public Almacen(String nombre, String tipoUbicacion, String direccionReferencia) {
		this.nombre = nombre;
		this.tipoUbicacion = tipoUbicacion;
		this.direccionReferencia = direccionReferencia;
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public String getTipoUbicacion() {
		return tipoUbicacion;
	}

	public String getDireccionReferencia() {
		return direccionReferencia;
	}

	public boolean isEstadoActivo() {
		return estadoActivo;
	}

	/** Activa o desactiva la disponibilidad de la ubicación para operaciones de inventario. */
	public void cambiarEstado(boolean activo) {
		this.estadoActivo = activo;
	}
}
