package com.migal.smartops.comercial.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Generated;

/**
 * Cliente de la ferretería (tabla {@code cliente}). El documento de identificación es opcional en las operaciones
 * de mostrador y su unicidad se aplica solo a los documentos informados (V11). La desactivación es lógica mediante
 * {@code estadoActivo} y conserva el historial.
 */
@Entity
@Table(name = "cliente")
public class Cliente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_cliente")
	private Long id;

	@Column(name = "tipo_documento", length = 10)
	private String tipoDocumento;

	@Column(name = "numero_documento", length = 20)
	private String numeroDocumento;

	@Column(name = "nombre_razon_social", nullable = false, length = 200)
	private String nombreRazonSocial;

	@Column(name = "telefono_contacto", length = 30)
	private String telefonoContacto;

	@Column(name = "direccion_referencia", length = 255)
	private String direccionReferencia;

	@Column(name = "estado_activo", nullable = false)
	private boolean estadoActivo = true;

	@Generated
	@Column(name = "fecha_registro", insertable = false, updatable = false)
	private Instant fechaRegistro;

	protected Cliente() {
		// Requerido por JPA.
	}

	/** Crea un cliente activo. El tipo y el número de documento son nulos en los clientes de mostrador. */
	public Cliente(TipoDocumentoCliente tipoDocumento, String numeroDocumento, String nombreRazonSocial,
			String telefonoContacto, String direccionReferencia) {
		actualizar(tipoDocumento, numeroDocumento, nombreRazonSocial, telefonoContacto, direccionReferencia);
	}

	/** Reemplaza los datos registrables del cliente. La vigencia no se modifica. */
	public void actualizar(TipoDocumentoCliente tipoDocumento, String numeroDocumento, String nombreRazonSocial,
			String telefonoContacto, String direccionReferencia) {
		this.tipoDocumento = tipoDocumento == null ? null : tipoDocumento.valor();
		this.numeroDocumento = numeroDocumento;
		this.nombreRazonSocial = nombreRazonSocial;
		this.telefonoContacto = telefonoContacto;
		this.direccionReferencia = direccionReferencia;
	}

	/** Activa o desactiva el cliente sin eliminar su registro. */
	public void cambiarEstado(boolean activo) {
		this.estadoActivo = activo;
	}

	public Long getId() {
		return id;
	}

	/** Tipo de documento, o nulo en los clientes de mostrador. */
	public TipoDocumentoCliente getTipoDocumento() {
		return tipoDocumento == null ? null : TipoDocumentoCliente.de(tipoDocumento);
	}

	public String getNumeroDocumento() {
		return numeroDocumento;
	}

	public String getNombreRazonSocial() {
		return nombreRazonSocial;
	}

	public String getTelefonoContacto() {
		return telefonoContacto;
	}

	public String getDireccionReferencia() {
		return direccionReferencia;
	}

	public boolean isEstadoActivo() {
		return estadoActivo;
	}

	public Instant getFechaRegistro() {
		return fechaRegistro;
	}
}
