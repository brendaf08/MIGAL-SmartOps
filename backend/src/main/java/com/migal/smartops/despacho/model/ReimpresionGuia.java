package com.migal.smartops.despacho.model;

import com.migal.smartops.seguridad.model.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Generated;

/**
 * Reimpresión de una guía de despacho (tabla {@code reimpresion_guia}, migración V12). Registra el usuario, la fecha y
 * el motivo obligatorio. Es de solo anexión: todas las columnas son no actualizables y la base de datos rechaza
 * cualquier modificación o eliminación. La reimpresión no modifica el estado de la guía ni el inventario.
 */
@Entity
@Table(name = "reimpresion_guia")
public class ReimpresionGuia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_reimpresion")
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "id_guia", nullable = false, updatable = false)
	private GuiaDespacho guia;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "id_usuario", nullable = false, updatable = false)
	private Usuario usuario;

	@Generated
	@Column(name = "fecha_reimpresion", insertable = false, updatable = false)
	private Instant fechaReimpresion;

	@Column(name = "motivo", nullable = false, updatable = false)
	private String motivo;

	protected ReimpresionGuia() {
		// Requerido por JPA.
	}

	/** Registra una reimpresión de la guía con su motivo, que no debe estar vacío. */
	public ReimpresionGuia(GuiaDespacho guia, Usuario usuario, String motivo) {
		this.guia = guia;
		this.usuario = usuario;
		this.motivo = motivo;
	}

	public Long getId() {
		return id;
	}

	public GuiaDespacho getGuia() {
		return guia;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public Instant getFechaReimpresion() {
		return fechaReimpresion;
	}

	public String getMotivo() {
		return motivo;
	}
}
