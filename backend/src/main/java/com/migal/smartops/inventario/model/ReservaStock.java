package com.migal.smartops.inventario.model;

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
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.Generated;

/**
 * Cantidad de stock comprometida por una línea de venta desde un inventario (tabla {@code reserva_stock}).
 * Una reserva activa pasa a liberada (la venta se cancela) o a consumida (el despacho la convierte en salida
 * de Kardex); los estados finales no admiten nuevas transiciones. La línea de venta se referencia por su
 * identificador porque pertenece al módulo comercial.
 */
@Entity
@Table(name = "reserva_stock")
public class ReservaStock {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_reserva")
	private Long id;

	@Column(name = "id_detalle_venta", nullable = false, updatable = false)
	private Long idDetalleVenta;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_inventario", nullable = false, updatable = false)
	private Inventario inventario;

	@Column(name = "id_producto", nullable = false, updatable = false)
	private Long idProducto;

	@Column(name = "cantidad_reservada", nullable = false, precision = 14, scale = 3, updatable = false)
	private BigDecimal cantidadReservada;

	@Column(name = "estado", nullable = false, length = 20)
	private String estado = EstadoReserva.ACTIVA.valor();

	@Generated
	@Column(name = "fecha_reserva", insertable = false, updatable = false)
	private Instant fechaReserva;

	@Column(name = "fecha_cierre")
	private Instant fechaCierre;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_usuario_responsable", nullable = false, updatable = false)
	private Usuario usuarioResponsable;

	protected ReservaStock() {
		// Requerido por JPA.
	}

	public ReservaStock(long idDetalleVenta, Inventario inventario, long idProducto, BigDecimal cantidadReservada,
			Usuario usuarioResponsable) {
		this.idDetalleVenta = idDetalleVenta;
		this.inventario = inventario;
		this.idProducto = idProducto;
		this.cantidadReservada = cantidadReservada;
		this.usuarioResponsable = usuarioResponsable;
	}

	public Long getId() {
		return id;
	}

	public Long getIdDetalleVenta() {
		return idDetalleVenta;
	}

	public Inventario getInventario() {
		return inventario;
	}

	public Long getIdProducto() {
		return idProducto;
	}

	public BigDecimal getCantidadReservada() {
		return cantidadReservada;
	}

	public EstadoReserva getEstado() {
		return EstadoReserva.de(estado);
	}

	public Instant getFechaReserva() {
		return fechaReserva;
	}

	public Instant getFechaCierre() {
		return fechaCierre;
	}

	public Usuario getUsuarioResponsable() {
		return usuarioResponsable;
	}

	/** Cierra la reserva activa como liberada. */
	public void liberar(Instant ahora) {
		cerrar(EstadoReserva.LIBERADA, ahora);
	}

	/** Cierra la reserva activa como consumida por un despacho. */
	public void consumir(Instant ahora) {
		cerrar(EstadoReserva.CONSUMIDA, ahora);
	}

	private void cerrar(EstadoReserva nuevo, Instant ahora) {
		if (getEstado() != EstadoReserva.ACTIVA) {
			throw new IllegalStateException("La reserva " + id + " ya está cerrada.");
		}
		this.estado = nuevo.valor();
		this.fechaCierre = ahora;
	}
}
