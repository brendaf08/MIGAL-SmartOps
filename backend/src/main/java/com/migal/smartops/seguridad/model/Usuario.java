package com.migal.smartops.seguridad.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Generated;

/**
 * Cuenta de acceso al sistema (tabla {@code usuario}). La contraseña se conserva únicamente como
 * representación hash. La desactivación es lógica mediante {@code estadoActivo}.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_usuario")
	private Long id;

	@Column(name = "nombres", nullable = false, length = 100)
	private String nombres;

	@Column(name = "apellidos", nullable = false, length = 100)
	private String apellidos;

	@Column(name = "username", nullable = false, length = 50, updatable = false)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "rol", nullable = false, length = 20)
	private RolUsuario rol;

	@Column(name = "estado_activo", nullable = false)
	private boolean estadoActivo = true;

	@Generated
	@Column(name = "fecha_registro", insertable = false, updatable = false)
	private Instant fechaRegistro;

	@Column(name = "fecha_actualizacion")
	private Instant fechaActualizacion;

	protected Usuario() {
		// Requerido por JPA.
	}

	public Usuario(String nombres, String apellidos, String username, String passwordHash, RolUsuario rol) {
		this.nombres = nombres;
		this.apellidos = apellidos;
		this.username = username;
		this.passwordHash = passwordHash;
		this.rol = rol;
	}

	public Long getId() {
		return id;
	}

	public String getNombres() {
		return nombres;
	}

	public String getApellidos() {
		return apellidos;
	}

	public String getUsername() {
		return username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public RolUsuario getRol() {
		return rol;
	}

	public boolean isEstadoActivo() {
		return estadoActivo;
	}

	public Instant getFechaRegistro() {
		return fechaRegistro;
	}

	public Instant getFechaActualizacion() {
		return fechaActualizacion;
	}

	/** Actualiza los nombres, los apellidos y el rol, y registra el momento de la modificación. */
	public void actualizar(String nombres, String apellidos, RolUsuario rol, Instant ahora) {
		this.nombres = nombres;
		this.apellidos = apellidos;
		this.rol = rol;
		this.fechaActualizacion = ahora;
	}

	/** Activa o desactiva el acceso del usuario y registra el momento de la modificación. */
	public void cambiarEstado(boolean activo, Instant ahora) {
		this.estadoActivo = activo;
		this.fechaActualizacion = ahora;
	}

	/** Reemplaza la representación hash de la contraseña. */
	public void cambiarPasswordHash(String passwordHash, Instant ahora) {
		this.passwordHash = passwordHash;
		this.fechaActualizacion = ahora;
	}
}
