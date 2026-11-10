package com.migal.smartops.agenteia.model;

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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Conversación del usuario con el agente de IA (tabla {@code conversacion_agente}, migración V14). Pertenece a un único
 * usuario y conserva sus mensajes en orden cronológico. La eliminación borra los mensajes en cascada, según la excepción
 * documentada en la migración V14.
 */
@Entity
@Table(name = "conversacion_agente")
public class ConversacionAgente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_conversacion")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_usuario", nullable = false, updatable = false)
	private Usuario usuario;

	@Column(name = "titulo", nullable = false, length = 200, updatable = false)
	private String titulo;

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private Instant fechaCreacion;

	@Column(name = "fecha_ultimo_mensaje", nullable = false)
	private Instant fechaUltimoMensaje;

	@OneToMany(mappedBy = "conversacion", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("fecha ASC, id ASC")
	private List<MensajeAgente> mensajes = new ArrayList<>();

	protected ConversacionAgente() {
		// Requerido por JPA.
	}

	/** Conversación nueva, sin mensajes. El título se genera a partir del primer mensaje del usuario. */
	public ConversacionAgente(Usuario usuario, String titulo, Instant ahora) {
		this.usuario = usuario;
		this.titulo = titulo;
		this.fechaCreacion = ahora;
		this.fechaUltimoMensaje = ahora;
	}

	/** Agrega un mensaje a la conversación y actualiza la fecha del último mensaje. */
	public void agregarMensaje(MensajeAgente mensaje) {
		mensajes.add(mensaje);
		mensaje.asignarConversacion(this);
		this.fechaUltimoMensaje = mensaje.getFecha();
	}

	/** Indica si la conversación pertenece al usuario indicado. */
	public boolean perteneceA(long idUsuario) {
		return usuario.getId() != null && usuario.getId() == idUsuario;
	}

	public Long getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public String getTitulo() {
		return titulo;
	}

	public Instant getFechaCreacion() {
		return fechaCreacion;
	}

	public Instant getFechaUltimoMensaje() {
		return fechaUltimoMensaje;
	}

	public List<MensajeAgente> getMensajes() {
		return mensajes;
	}
}
