package com.migal.smartops.comercial.model;

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

/**
 * Cambio de estado de una cotización que merece trazabilidad: la modificación de una cotización ya aceptada o validada
 * (que vuelve a emitida) y su cancelación. Guarda quién lo hizo, cuándo, de qué estado a cuál y el motivo.
 */
@Entity
@Table(name = "cotizacion_historial")
public class CambioCotizacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_historial")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_cotizacion", nullable = false, updatable = false)
	private Cotizacion cotizacion;

	@Column(name = "fecha", nullable = false, updatable = false)
	private Instant fecha;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_usuario", nullable = false, updatable = false)
	private Usuario usuario;

	@Column(name = "estado_anterior", nullable = false, updatable = false, length = 20)
	private String estadoAnterior;

	@Column(name = "estado_nuevo", nullable = false, updatable = false, length = 20)
	private String estadoNuevo;

	@Column(name = "motivo", nullable = false, updatable = false)
	private String motivo;

	protected CambioCotizacion() {
		// Requerido por JPA.
	}

	CambioCotizacion(Cotizacion cotizacion, Usuario usuario, Instant fecha, EstadoCotizacion anterior,
			EstadoCotizacion nuevo, String motivo) {
		this.cotizacion = cotizacion;
		this.usuario = usuario;
		this.fecha = fecha;
		this.estadoAnterior = anterior.valor();
		this.estadoNuevo = nuevo.valor();
		this.motivo = motivo;
	}

	public Long getId() {
		return id;
	}

	public Instant getFecha() {
		return fecha;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public String getEstadoAnterior() {
		return estadoAnterior;
	}

	public String getEstadoNuevo() {
		return estadoNuevo;
	}

	public String getMotivo() {
		return motivo;
	}
}
