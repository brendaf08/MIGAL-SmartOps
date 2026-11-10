package com.migal.smartops.comercial.model;

import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.seguridad.model.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cotización emitida a un cliente registrado (tabla {@code cotizacion}, CU-09). No reserva ni modifica existencias.
 * El subtotal y el total son derivados: el total es el subtotal de las líneas menos el descuento. Los métodos de
 * esta entidad aplican los cambios de estado; la capa de servicio verifica las transiciones permitidas.
 */
@Entity
@Table(name = "cotizacion")
public class Cotizacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_cotizacion")
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "id_cliente", nullable = false)
	private Cliente cliente;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "id_usuario_responsable", nullable = false)
	private Usuario usuarioResponsable;

	@Column(name = "fecha_emision", nullable = false)
	private Instant fechaEmision;

	@Column(name = "fecha_vencimiento", nullable = false)
	private LocalDate fechaVencimiento;

	@Column(name = "condiciones_comerciales")
	private String condicionesComerciales;

	@Column(name = "descuento", nullable = false, precision = 14, scale = 2)
	private BigDecimal descuento;

	@Column(name = "estado", nullable = false, length = 20)
	private String estado;

	@Column(name = "fecha_aceptacion_cliente")
	private Instant fechaAceptacionCliente;

	@Column(name = "fecha_validacion_migal")
	private Instant fechaValidacionMigal;

	@Column(name = "origen_rechazo", length = 10)
	private String origenRechazo;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "id_usuario_rechazo")
	private Usuario usuarioRechazo;

	@Column(name = "fecha_rechazo")
	private Instant fechaRechazo;

	@Column(name = "motivo_rechazo")
	private String motivoRechazo;

	@OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	private List<DetalleCotizacion> lineas = new ArrayList<>();

	protected Cotizacion() {
		// Requerido por JPA.
	}

	/** Crea una cotización en estado emitida, sin líneas. */
	public Cotizacion(Cliente cliente, Usuario usuarioResponsable, Instant fechaEmision, LocalDate fechaVencimiento,
			String condicionesComerciales, BigDecimal descuento) {
		this.cliente = cliente;
		this.usuarioResponsable = usuarioResponsable;
		this.fechaEmision = fechaEmision;
		this.estado = EstadoCotizacion.EMITIDA.valor();
		actualizarDatos(cliente, fechaVencimiento, condicionesComerciales, descuento);
	}

	/** Reemplaza el cliente, las condiciones, el vencimiento y el descuento. Las líneas se manejan aparte. */
	public void actualizarDatos(Cliente cliente, LocalDate fechaVencimiento, String condicionesComerciales,
			BigDecimal descuento) {
		this.cliente = cliente;
		this.fechaVencimiento = fechaVencimiento;
		this.condicionesComerciales = condicionesComerciales;
		this.descuento = descuento.setScale(2, RoundingMode.HALF_UP);
	}

	/** Agrega una línea. El producto no debe repetirse; la capa de servicio lo verifica. */
	public void agregarLinea(Producto producto, BigDecimal cantidad, BigDecimal precioUnitario) {
		lineas.add(new DetalleCotizacion(this, producto, cantidad, precioUnitario));
	}

	/** Quita una línea de la cotización. */
	public void quitarLinea(DetalleCotizacion linea) {
		lineas.remove(linea);
	}

	/** Cambia el estado comercial a un valor sin verificar la transición. */
	public void cambiarEstado(EstadoCotizacion nuevo) {
		this.estado = nuevo.valor();
	}

	/** Registra la aceptación del cliente y cambia el estado a aceptada. */
	public void registrarAceptacion(Instant momento) {
		cambiarEstado(EstadoCotizacion.ACEPTADA);
		this.fechaAceptacionCliente = momento;
	}

	/** Registra la validación de viabilidad por MIGAL y cambia el estado a validada. */
	public void registrarValidacion(Instant momento) {
		cambiarEstado(EstadoCotizacion.VALIDADA);
		this.fechaValidacionMigal = momento;
	}

	/** Registra el rechazo: el origen, el usuario, la fecha y el motivo, y cambia el estado a rechazada. */
	public void registrarRechazo(OrigenRechazoCotizacion origen, Usuario usuario, Instant momento, String motivo) {
		cambiarEstado(EstadoCotizacion.RECHAZADA);
		this.origenRechazo = origen.valor();
		this.usuarioRechazo = usuario;
		this.fechaRechazo = momento;
		this.motivoRechazo = motivo;
	}

	/** Cambia el estado a vencida. */
	public void vencer() {
		cambiarEstado(EstadoCotizacion.VENCIDA);
	}

	/** Subtotal derivado: suma de los subtotales de las líneas. */
	public BigDecimal subtotal() {
		return lineas.stream().map(DetalleCotizacion::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
	}

	/** Total derivado: subtotal menos el descuento. */
	public BigDecimal total() {
		return subtotal().subtract(descuento);
	}

	public Long getId() {
		return id;
	}

	public Cliente getCliente() {
		return cliente;
	}

	public Usuario getUsuarioResponsable() {
		return usuarioResponsable;
	}

	public Instant getFechaEmision() {
		return fechaEmision;
	}

	public LocalDate getFechaVencimiento() {
		return fechaVencimiento;
	}

	public String getCondicionesComerciales() {
		return condicionesComerciales;
	}

	public BigDecimal getDescuento() {
		return descuento;
	}

	public EstadoCotizacion getEstado() {
		return EstadoCotizacion.de(estado);
	}

	public Instant getFechaAceptacionCliente() {
		return fechaAceptacionCliente;
	}

	public Instant getFechaValidacionMigal() {
		return fechaValidacionMigal;
	}

	/** Origen del rechazo, o nulo mientras la cotización no esté rechazada. */
	public OrigenRechazoCotizacion getOrigenRechazo() {
		return origenRechazo == null ? null : OrigenRechazoCotizacion.de(origenRechazo);
	}

	public Usuario getUsuarioRechazo() {
		return usuarioRechazo;
	}

	public Instant getFechaRechazo() {
		return fechaRechazo;
	}

	public String getMotivoRechazo() {
		return motivoRechazo;
	}

	/** Líneas de solo lectura. Las modificaciones se realizan mediante los métodos de esta entidad. */
	public List<DetalleCotizacion> getLineas() {
		return Collections.unmodifiableList(lineas);
	}
}
