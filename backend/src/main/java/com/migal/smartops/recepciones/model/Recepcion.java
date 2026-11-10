package com.migal.smartops.recepciones.model;

import com.migal.smartops.abastecimiento.model.OrdenCompra;
import com.migal.smartops.catalogo.model.Almacen;
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
import org.hibernate.annotations.Generated;

/**
 * Recepción de mercadería contra una orden de compra (tabla {@code orden_recepcion}, migraciones V5 y V11). El código
 * visible {@code R-0012} lo genera la base de datos. La recepción nace pendiente; la validación la pasa a validada y
 * registra la fecha de validación. Una recepción validada no admite cambios.
 */
@Entity
@Table(name = "orden_recepcion")
public class Recepcion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_recepcion")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_orden", nullable = false, updatable = false)
	private OrdenCompra orden;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_usuario_responsable", nullable = false, updatable = false)
	private Usuario usuarioResponsable;

	@Generated
	@Column(name = "codigo", insertable = false, updatable = false)
	private String codigo;

	@Column(name = "fecha_recepcion", nullable = false)
	private LocalDate fechaRecepcion;

	@Column(name = "estado", nullable = false, length = 20)
	private String estado;

	@Column(name = "fecha_validacion")
	private Instant fechaValidacion;

	@Column(name = "observaciones")
	private String observaciones;

	@OneToMany(mappedBy = "recepcion", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<DetalleRecepcion> lineas = new ArrayList<>();

	protected Recepcion() {
		// Requerido por JPA.
	}

	/** Recepción nueva en estado pendiente, sin líneas. Las líneas se agregan con {@link #reemplazarLineas}. */
	public Recepcion(OrdenCompra orden, Usuario usuarioResponsable, LocalDate fechaRecepcion, String observaciones) {
		this.orden = orden;
		this.usuarioResponsable = usuarioResponsable;
		this.fechaRecepcion = fechaRecepcion;
		this.observaciones = observaciones;
		this.estado = EstadoRecepcion.PENDIENTE.valor();
	}

	/** Modifica la fecha y las observaciones de una recepción pendiente. */
	public void actualizar(LocalDate fechaRecepcion, String observaciones) {
		this.fechaRecepcion = fechaRecepcion;
		this.observaciones = observaciones;
	}

	/** Pasa la recepción a validada y registra el momento de la validación. */
	public void validar(Instant ahora) {
		this.estado = EstadoRecepcion.VALIDADA.valor();
		this.fechaValidacion = ahora;
	}

	/**
	 * Reemplaza las líneas de la recepción. Las líneas de la misma combinación de producto y ubicación se actualizan en su
	 * lugar, para no reinsertar una clave que aún existe; las demás se eliminan.
	 */
	public void reemplazarLineas(List<NuevaLinea> nuevas) {
		Map<Clave, DetalleRecepcion> existentes = new HashMap<>();
		lineas.forEach(l -> existentes.put(new Clave(l.getProducto().getId(), l.getAlmacen().getId()), l));
		List<DetalleRecepcion> resultado = new ArrayList<>();
		for (NuevaLinea nueva : nuevas) {
			DetalleRecepcion existente = existentes.remove(new Clave(nueva.producto().getId(), nueva.almacen().getId()));
			if (existente == null) {
				resultado.add(new DetalleRecepcion(this, nueva.producto(), nueva.almacen(), nueva.cantidad()));
			} else {
				existente.actualizar(nueva.cantidad());
				resultado.add(existente);
			}
		}
		lineas.retainAll(resultado);
		resultado.stream().filter(l -> !lineas.contains(l)).forEach(lineas::add);
	}

	/** Datos de una línea nueva o modificada. */
	public record NuevaLinea(Producto producto, Almacen almacen, BigDecimal cantidad) {
	}

	/** Clave natural de una línea: producto y ubicación. */
	private record Clave(Long idProducto, Long idAlmacen) {
	}

	public Long getId() {
		return id;
	}

	public OrdenCompra getOrden() {
		return orden;
	}

	public Usuario getUsuarioResponsable() {
		return usuarioResponsable;
	}

	public String getCodigo() {
		return codigo;
	}

	public LocalDate getFechaRecepcion() {
		return fechaRecepcion;
	}

	public EstadoRecepcion getEstado() {
		return EstadoRecepcion.de(estado);
	}

	public Instant getFechaValidacion() {
		return fechaValidacion;
	}

	public String getObservaciones() {
		return observaciones;
	}

	public List<DetalleRecepcion> getLineas() {
		return lineas;
	}
}
