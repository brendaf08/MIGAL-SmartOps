package com.migal.smartops.abastecimiento.model;

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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hibernate.annotations.Generated;

/**
 * Orden de compra registrada con un proveedor (tabla {@code orden_compra}, migraciones V5 y V11). El código visible
 * {@code OC-0087} lo genera la base de datos a partir del identificador, y la fecha de emisión la asigna el servidor.
 * El estado de cumplimiento se actualiza al validar recepciones; el registro de la orden no modifica el inventario.
 */
@Entity
@Table(name = "orden_compra")
public class OrdenCompra {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_orden")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_proveedor", nullable = false, updatable = false)
	private Proveedor proveedor;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_usuario_responsable", nullable = false, updatable = false)
	private Usuario usuarioResponsable;

	@Generated
	@Column(name = "codigo", insertable = false, updatable = false)
	private String codigo;

	@Generated
	@Column(name = "fecha_emision", insertable = false, updatable = false)
	private Instant fechaEmision;

	@Column(name = "estado", nullable = false, length = 20)
	private String estado;

	@Column(name = "fecha_entrega_estimada")
	private LocalDate fechaEntregaEstimada;

	@Column(name = "observaciones")
	private String observaciones;

	@Column(name = "modalidad_pago", length = 100)
	private String modalidadPago;

	@OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<DetalleCompra> lineas = new ArrayList<>();

	protected OrdenCompra() {
		// Requerido por JPA.
	}

	/** Orden nueva en estado pendiente, sin líneas. Las líneas se agregan con {@link #reemplazarLineas}. */
	public OrdenCompra(Proveedor proveedor, Usuario usuarioResponsable, LocalDate fechaEntregaEstimada,
			String observaciones, String modalidadPago) {
		this.proveedor = proveedor;
		this.usuarioResponsable = usuarioResponsable;
		this.estado = EstadoOrdenCompra.PENDIENTE.valor();
		this.fechaEntregaEstimada = fechaEntregaEstimada;
		this.observaciones = observaciones;
		this.modalidadPago = modalidadPago;
	}

	/** Modifica los datos generales de la orden. El proveedor no cambia. */
	public void actualizar(LocalDate fechaEntregaEstimada, String observaciones, String modalidadPago) {
		this.fechaEntregaEstimada = fechaEntregaEstimada;
		this.observaciones = observaciones;
		this.modalidadPago = modalidadPago;
	}

	/** Anexa una observación al cierre, separada del texto existente. */
	public void anexarObservacion(String texto) {
		this.observaciones = observaciones == null || observaciones.isBlank() ? texto : observaciones + "\n" + texto;
	}

	/** Cambia el estado de cumplimiento. */
	public void cambiarEstado(EstadoOrdenCompra nuevo) {
		this.estado = nuevo.valor();
	}

	/**
	 * Reemplaza las líneas de la orden. Las líneas de productos que permanecen se actualizan en su lugar, para no
	 * reinsertar una clave que aún existe; las de productos omitidos se eliminan.
	 */
	public void reemplazarLineas(List<DetalleCompra> nuevas) {
		Map<Long, DetalleCompra> existentes = lineas.stream()
				.collect(Collectors.toMap(l -> l.getProducto().getId(), l -> l, (a, b) -> a, HashMap::new));
		List<DetalleCompra> resultado = new ArrayList<>();
		for (DetalleCompra nueva : nuevas) {
			DetalleCompra existente = existentes.remove(nueva.getProducto().getId());
			if (existente == null) {
				resultado.add(nueva);
			} else {
				existente.actualizar(nueva.getCantidadComprada(), nueva.getCostoUnitario());
				resultado.add(existente);
			}
		}
		lineas.retainAll(resultado);
		resultado.stream().filter(l -> !lineas.contains(l)).forEach(lineas::add);
	}

	/** Crea una línea de la orden para un producto. Debe agregarse con {@link #reemplazarLineas}. */
	public DetalleCompra nuevaLinea(Producto producto, BigDecimal cantidadComprada, BigDecimal costoUnitario) {
		return new DetalleCompra(this, producto, cantidadComprada, costoUnitario);
	}

	/** Total derivado de la orden: suma de los subtotales de sus líneas. No se almacena. */
	public BigDecimal total() {
		return lineas.stream().map(DetalleCompra::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public Long getId() {
		return id;
	}

	public Proveedor getProveedor() {
		return proveedor;
	}

	public Usuario getUsuarioResponsable() {
		return usuarioResponsable;
	}

	public String getCodigo() {
		return codigo;
	}

	public Instant getFechaEmision() {
		return fechaEmision;
	}

	public EstadoOrdenCompra getEstado() {
		return EstadoOrdenCompra.de(estado);
	}

	public LocalDate getFechaEntregaEstimada() {
		return fechaEntregaEstimada;
	}

	public String getObservaciones() {
		return observaciones;
	}

	public String getModalidadPago() {
		return modalidadPago;
	}

	public List<DetalleCompra> getLineas() {
		return lineas;
	}
}
