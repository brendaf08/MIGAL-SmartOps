package com.migal.smartops.agenteia.model;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Mensaje de una conversación, del usuario o del agente (tabla {@code mensaje_agente}, migración V14). Las herramientas
 * invocadas y los registros consultados se conservan como arreglos JSON; su estructura la valida la capa de servicios.
 * Los mensajes son inmutables una vez registrados.
 */
@Entity
@Table(name = "mensaje_agente")
public class MensajeAgente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_mensaje")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_conversacion", nullable = false, updatable = false)
	private ConversacionAgente conversacion;

	@Column(name = "rol", nullable = false, length = 10, updatable = false)
	private String rol;

	@Column(name = "texto", nullable = false, updatable = false)
	private String texto;

	@Column(name = "incierta", nullable = false, updatable = false)
	private boolean incierta;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "herramientas", nullable = false, updatable = false)
	private String herramientas;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "referencias", nullable = false, updatable = false)
	private String referencias;

	@Column(name = "fecha", nullable = false, updatable = false)
	private Instant fecha;

	protected MensajeAgente() {
		// Requerido por JPA.
	}

	/**
	 * Mensaje nuevo. Los argumentos {@code herramientasJson} y {@code referenciasJson} son arreglos JSON; para los
	 * mensajes del usuario deben ser {@code []}, según la restricción de la migración V14.
	 */
	public MensajeAgente(RolMensaje rol, String texto, boolean incierta, String herramientasJson, String referenciasJson,
			Instant fecha) {
		this.rol = rol.valor();
		this.texto = texto;
		this.incierta = incierta;
		this.herramientas = herramientasJson;
		this.referencias = referenciasJson;
		this.fecha = fecha;
	}

	void asignarConversacion(ConversacionAgente conversacion) {
		this.conversacion = conversacion;
	}

	public Long getId() {
		return id;
	}

	public ConversacionAgente getConversacion() {
		return conversacion;
	}

	public RolMensaje getRol() {
		return RolMensaje.de(rol);
	}

	public String getTexto() {
		return texto;
	}

	public boolean isIncierta() {
		return incierta;
	}

	public String getHerramientas() {
		return herramientas;
	}

	public String getReferencias() {
		return referencias;
	}

	public Instant getFecha() {
		return fecha;
	}
}
