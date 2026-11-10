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
import java.util.UUID;
import org.hibernate.annotations.Generated;

/**
 * Movimiento del Kardex (tabla {@code movimiento_kardex}). Es de solo anexión: todas las columnas son no
 * actualizables y la base de datos rechaza cualquier modificación o eliminación. Una corrección se registra
 * como un movimiento compensatorio de origen ajuste que referencia el corregido.
 */
@Entity
@Table(name = "movimiento_kardex")
public class MovimientoKardex {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_movimiento")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_inventario", nullable = false, updatable = false)
	private Inventario inventario;

	@Generated
	@Column(name = "fecha_hora", insertable = false, updatable = false)
	private Instant fechaHora;

	@Column(name = "tipo_operacion", nullable = false, length = 10, updatable = false)
	private String tipoOperacion;

	@Column(name = "cantidad", nullable = false, precision = 14, scale = 3, updatable = false)
	private BigDecimal cantidad;

	@Column(name = "motivo", updatable = false)
	private String motivo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_usuario_responsable", nullable = false, updatable = false)
	private Usuario usuarioResponsable;

	@Column(name = "origen_movimiento", nullable = false, length = 20, updatable = false)
	private String origenMovimiento;

	@Column(name = "id_guia", updatable = false)
	private Long idGuia;

	@Column(name = "id_recepcion", updatable = false)
	private Long idRecepcion;

	@Column(name = "referencia_traslado", updatable = false)
	private UUID referenciaTraslado;

	@Column(name = "id_movimiento_corregido", updatable = false)
	private Long idMovimientoCorregido;

	protected MovimientoKardex() {
		// Requerido por JPA.
	}

	private MovimientoKardex(Inventario inventario, TipoOperacion tipo, BigDecimal cantidad, String motivo,
			Usuario responsable, OrigenMovimiento origen) {
		this.inventario = inventario;
		this.tipoOperacion = tipo.valor();
		this.cantidad = cantidad;
		this.motivo = motivo;
		this.usuarioResponsable = responsable;
		this.origenMovimiento = origen.valor();
	}

	/** Ajuste manual, con motivo obligatorio y, si compensa otro movimiento, su referencia. */
	public static MovimientoKardex ajuste(Inventario inventario, TipoOperacion tipo, BigDecimal cantidad, String motivo,
			Usuario responsable, Long idMovimientoCorregido) {
		MovimientoKardex m = new MovimientoKardex(inventario, tipo, cantidad, motivo, responsable, OrigenMovimiento.AJUSTE);
		m.idMovimientoCorregido = idMovimientoCorregido;
		return m;
	}

	/** Una de las dos patas de un traslado; ambas comparten la referencia. */
	public static MovimientoKardex traslado(Inventario inventario, TipoOperacion tipo, BigDecimal cantidad, String motivo,
			Usuario responsable, UUID referencia) {
		MovimientoKardex m = new MovimientoKardex(inventario, tipo, cantidad, motivo, responsable, OrigenMovimiento.TRASLADO);
		m.referenciaTraslado = referencia;
		return m;
	}

	/** Salida por despacho, vinculada a su guía. */
	public static MovimientoKardex despacho(Inventario inventario, BigDecimal cantidad, Usuario responsable, long idGuia) {
		MovimientoKardex m = new MovimientoKardex(inventario, TipoOperacion.SALIDA, cantidad, null, responsable,
				OrigenMovimiento.DESPACHO);
		m.idGuia = idGuia;
		return m;
	}

	/** Entrada por recepción, vinculada a su orden de recepción. */
	public static MovimientoKardex recepcion(Inventario inventario, BigDecimal cantidad, Usuario responsable,
			long idRecepcion) {
		MovimientoKardex m = new MovimientoKardex(inventario, TipoOperacion.ENTRADA, cantidad, null, responsable,
				OrigenMovimiento.RECEPCION);
		m.idRecepcion = idRecepcion;
		return m;
	}

	public Long getId() {
		return id;
	}

	public Inventario getInventario() {
		return inventario;
	}

	public Instant getFechaHora() {
		return fechaHora;
	}

	public TipoOperacion getTipoOperacion() {
		return TipoOperacion.de(tipoOperacion);
	}

	public BigDecimal getCantidad() {
		return cantidad;
	}

	public String getMotivo() {
		return motivo;
	}

	public Usuario getUsuarioResponsable() {
		return usuarioResponsable;
	}

	public OrigenMovimiento getOrigenMovimiento() {
		return OrigenMovimiento.de(origenMovimiento);
	}

	public Long getIdGuia() {
		return idGuia;
	}

	public Long getIdRecepcion() {
		return idRecepcion;
	}

	public UUID getReferenciaTraslado() {
		return referenciaTraslado;
	}

	public Long getIdMovimientoCorregido() {
		return idMovimientoCorregido;
	}
}
