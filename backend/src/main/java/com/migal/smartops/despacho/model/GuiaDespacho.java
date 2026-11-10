package com.migal.smartops.despacho.model;

import com.migal.smartops.comercial.model.Venta;
import com.migal.smartops.seguridad.model.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Generated;

/**
 * Guía de despacho de una venta (tabla {@code guia_despacho}, migraciones V6 y V12). Cada venta origina como máximo
 * una guía, que documenta la salida física completa de la mercadería. La emisión la crea en estado emitida; la
 * confirmación de la salida registra la fecha real y el usuario responsable; la entrega registra su fecha, y la
 * cancelación de la venta la anula. Cada método aplica una transición y verifica que sea permitida; la capa de
 * servicio la verifica antes para responder con el código de error del contrato.
 */
@Entity
@Table(name = "guia_despacho")
public class GuiaDespacho {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_guia")
	private Long id;

	/** Venta de origen. Única por guía, lo que garantiza una sola guía por venta. */
	@OneToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "id_venta", nullable = false, unique = true, updatable = false)
	private Venta venta;

	/** Código visible generado por la base de datos a partir del identificador, por ejemplo D-0031. */
	@Generated
	@Column(name = "codigo", insertable = false, updatable = false)
	private String codigo;

	@Column(name = "direccion_entrega", nullable = false, length = 255, updatable = false)
	private String direccionEntrega;

	@Column(name = "fecha_despacho")
	private LocalDate fechaDespacho;

	@Generated
	@Column(name = "fecha_emision", insertable = false, updatable = false)
	private Instant fechaEmision;

	@Column(name = "estado", nullable = false, length = 20)
	private String estado;

	@Column(name = "fecha_salida_real")
	private Instant fechaSalidaReal;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "id_usuario_responsable")
	private Usuario usuarioResponsable;

	@Column(name = "fecha_entrega")
	private Instant fechaEntrega;

	protected GuiaDespacho() {
		// Requerido por JPA.
	}

	/** Emite la guía de una venta en estado emitida. La dirección de entrega se copia de la venta. */
	public GuiaDespacho(Venta venta, String direccionEntrega) {
		this.venta = venta;
		this.direccionEntrega = direccionEntrega;
		this.estado = EstadoGuia.EMITIDA.valor();
	}

	/** Confirma la salida física completa. Solo una guía emitida admite esta transición. */
	public void confirmarSalida(Usuario responsable, Instant momento) {
		exigirEstado(EstadoGuia.EMITIDA, "confirmar la salida");
		this.estado = EstadoGuia.SALIDA_CONFIRMADA.valor();
		this.fechaSalidaReal = momento;
		this.usuarioResponsable = responsable;
	}

	/** Registra la entrega al cliente. Solo una guía con salida confirmada admite esta transición. */
	public void registrarEntrega(Instant momento) {
		exigirEstado(EstadoGuia.SALIDA_CONFIRMADA, "registrar la entrega");
		this.estado = EstadoGuia.ENTREGADA.valor();
		this.fechaEntrega = momento;
	}

	/** Anula la guía por cancelación de la venta. Una guía con salida confirmada no puede anularse. */
	public void anular() {
		EstadoGuia actual = getEstado();
		if (actual != EstadoGuia.EMITIDA && actual != EstadoGuia.PREPARACION) {
			throw new IllegalStateException("La guía " + id + " no permite anularse en estado " + estado + ".");
		}
		this.estado = EstadoGuia.ANULADA.valor();
	}

	private void exigirEstado(EstadoGuia esperado, String operacion) {
		if (getEstado() != esperado) {
			throw new IllegalStateException("La guía " + id + " no permite " + operacion + " en estado " + estado + ".");
		}
	}

	public Long getId() {
		return id;
	}

	public Venta getVenta() {
		return venta;
	}

	public String getCodigo() {
		return codigo;
	}

	public String getDireccionEntrega() {
		return direccionEntrega;
	}

	public LocalDate getFechaDespacho() {
		return fechaDespacho;
	}

	public Instant getFechaEmision() {
		return fechaEmision;
	}

	public EstadoGuia getEstado() {
		return EstadoGuia.de(estado);
	}

	public Instant getFechaSalidaReal() {
		return fechaSalidaReal;
	}

	public Usuario getUsuarioResponsable() {
		return usuarioResponsable;
	}

	public Instant getFechaEntrega() {
		return fechaEntrega;
	}
}
