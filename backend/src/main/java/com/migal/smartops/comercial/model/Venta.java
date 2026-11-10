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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Venta aprobada, originada en una cotización aceptada y validada (tabla {@code venta}, ADR-003). La venta solo
 * reserva existencias y no genera movimientos de Kardex. El subtotal y el total son derivados. La cobertura de cada
 * línea se calcula a partir de sus reservas. Los métodos de esta entidad aplican los cambios de estado; la capa de
 * servicio verifica las transiciones permitidas.
 */
@Entity
@Table(name = "venta")
public class Venta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_venta")
	private Long id;

	/** Cotización de origen. Única por venta, lo que garantiza una sola venta por cotización. */
	@OneToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "id_cotizacion", nullable = false, unique = true, updatable = false)
	private Cotizacion cotizacion;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "id_cliente", nullable = false, updatable = false)
	private Cliente cliente;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "id_usuario_responsable", nullable = false, updatable = false)
	private Usuario usuarioResponsable;

	@Column(name = "fecha_venta", nullable = false, updatable = false)
	private Instant fechaVenta;

	@Column(name = "descuento", nullable = false, precision = 14, scale = 2)
	private BigDecimal descuento;

	@Column(name = "estado", nullable = false, length = 30)
	private String estado;

	@Column(name = "fecha_cancelacion")
	private Instant fechaCancelacion;

	@Column(name = "motivo_cancelacion")
	private String motivoCancelacion;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "id_usuario_autorizante")
	private Usuario usuarioAutorizante;

	@OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	private List<DetalleVenta> lineas = new ArrayList<>();

	protected Venta() {
		// Requerido por JPA.
	}

	/**
	 * Crea una venta en estado pendiente de abastecimiento, sin líneas. El cliente se toma de la cotización de origen,
	 * que debe estar aceptada y validada; la capa de servicio lo verifica.
	 */
	public Venta(Cotizacion cotizacion, Usuario usuarioResponsable, Instant fechaVenta, BigDecimal descuento) {
		this.cotizacion = cotizacion;
		this.cliente = cotizacion.getCliente();
		this.usuarioResponsable = usuarioResponsable;
		this.fechaVenta = fechaVenta;
		this.descuento = descuento.setScale(2, RoundingMode.HALF_UP);
		this.estado = EstadoVenta.PENDIENTE_ABASTECIMIENTO.valor();
	}

	/** Agrega una línea. El producto no debe repetirse; la capa de servicio lo verifica. */
	public DetalleVenta agregarLinea(Producto producto, BigDecimal cantidad, BigDecimal precioUnitario) {
		DetalleVenta linea = new DetalleVenta(this, producto, cantidad, precioUnitario);
		lineas.add(linea);
		return linea;
	}

	/** Cambia el estado a un valor sin verificar la transición. */
	public void cambiarEstado(EstadoVenta nuevo) {
		this.estado = nuevo.valor();
	}

	/**
	 * Cancela la venta: el estado pasa a cancelada y se registran la fecha, el motivo y el usuario autorizante. Las
	 * reservas se liberan en la capa de servicio.
	 */
	public void cancelar(Usuario autorizante, Instant momento, String motivo) {
		cambiarEstado(EstadoVenta.CANCELADA);
		this.usuarioAutorizante = autorizante;
		this.fechaCancelacion = momento;
		this.motivoCancelacion = motivo;
	}

	/** Subtotal derivado: suma de los subtotales de las líneas. */
	public BigDecimal subtotal() {
		return lineas.stream().map(DetalleVenta::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
	}

	/** Total derivado: subtotal menos el descuento. */
	public BigDecimal total() {
		return subtotal().subtract(descuento);
	}

	public Long getId() {
		return id;
	}

	public Cotizacion getCotizacion() {
		return cotizacion;
	}

	public Cliente getCliente() {
		return cliente;
	}

	public Usuario getUsuarioResponsable() {
		return usuarioResponsable;
	}

	public Instant getFechaVenta() {
		return fechaVenta;
	}

	public BigDecimal getDescuento() {
		return descuento;
	}

	public EstadoVenta getEstado() {
		return EstadoVenta.de(estado);
	}

	public Instant getFechaCancelacion() {
		return fechaCancelacion;
	}

	public String getMotivoCancelacion() {
		return motivoCancelacion;
	}

	public Usuario getUsuarioAutorizante() {
		return usuarioAutorizante;
	}

	/** Líneas de solo lectura. Las modificaciones se realizan mediante los métodos de esta entidad. */
	public List<DetalleVenta> getLineas() {
		return Collections.unmodifiableList(lineas);
	}
}
