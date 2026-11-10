package com.migal.smartops.abastecimiento.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Generated;

/**
 * Proveedor del directorio de MIGAL (tabla {@code proveedor}, migración V2). El RUC identifica de forma única al
 * proveedor. La desactivación es lógica: el proveedor conserva su historial y no puede usarse en nuevas órdenes de
 * compra. La fecha de registro la asigna la base de datos.
 */
@Entity
@Table(name = "proveedor")
public class Proveedor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_proveedor")
	private Long id;

	@Column(name = "ruc", nullable = false, length = 11)
	private String ruc;

	@Column(name = "razon_social", nullable = false, length = 200)
	private String razonSocial;

	@Column(name = "contacto", nullable = false, length = 200)
	private String contacto;

	@Column(name = "condiciones")
	private String condiciones;

	@Column(name = "estado_activo", nullable = false)
	private boolean estadoActivo;

	@Generated
	@Column(name = "fecha_registro", insertable = false, updatable = false)
	private Instant fechaRegistro;

	protected Proveedor() {
		// Requerido por JPA.
	}

	public Proveedor(String ruc, String razonSocial, String contacto, String condiciones, boolean estadoActivo) {
		this.ruc = ruc;
		this.razonSocial = razonSocial;
		this.contacto = contacto;
		this.condiciones = condiciones;
		this.estadoActivo = estadoActivo;
	}

	/** Reemplaza los datos identificativos y de contacto. La vigencia se cambia con {@link #cambiarEstado(boolean)}. */
	public void actualizar(String ruc, String razonSocial, String contacto, String condiciones) {
		this.ruc = ruc;
		this.razonSocial = razonSocial;
		this.contacto = contacto;
		this.condiciones = condiciones;
	}

	/** Activa o desactiva el proveedor. La desactivación no elimina el historial. */
	public void cambiarEstado(boolean activo) {
		this.estadoActivo = activo;
	}

	public Long getId() {
		return id;
	}

	public String getRuc() {
		return ruc;
	}

	public String getRazonSocial() {
		return razonSocial;
	}

	public String getContacto() {
		return contacto;
	}

	public String getCondiciones() {
		return condiciones;
	}

	public boolean isEstadoActivo() {
		return estadoActivo;
	}

	public Instant getFechaRegistro() {
		return fechaRegistro;
	}
}
