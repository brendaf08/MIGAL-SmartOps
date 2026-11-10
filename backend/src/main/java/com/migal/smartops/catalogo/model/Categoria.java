package com.migal.smartops.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Categoría comercial de productos (tabla {@code categoria}). Las categorías forman una jerarquía de dos
 * niveles mediante la autorreferencia {@code id_categoria_padre}. La categoría determina si la viabilidad de
 * abastecimiento de sus productos se aprueba de forma automática.
 */
@Entity
@Table(name = "categoria")
public class Categoria {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_categoria")
	private Long id;

	@Column(name = "nombre", nullable = false, length = 100)
	private String nombre;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "id_categoria_padre")
	private Categoria padre;

	@Column(name = "aprobacion_automatica", nullable = false)
	private boolean aprobacionAutomatica;

	@Column(name = "estado_activo", nullable = false)
	private boolean estadoActivo = true;

	protected Categoria() {
		// Requerido por JPA.
	}

	/** Crea una categoría de primer nivel cuando el padre es nulo, o una subcategoría en caso contrario. */
	public Categoria(String nombre, Categoria padre, boolean aprobacionAutomatica) {
		this.nombre = nombre;
		this.padre = padre;
		this.aprobacionAutomatica = aprobacionAutomatica;
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	/** Categoría padre, o nulo en las categorías de primer nivel. */
	public Categoria getPadre() {
		return padre;
	}

	public boolean isAprobacionAutomatica() {
		return aprobacionAutomatica;
	}

	public boolean isEstadoActivo() {
		return estadoActivo;
	}
}
