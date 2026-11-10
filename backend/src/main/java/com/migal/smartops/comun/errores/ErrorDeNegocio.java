package com.migal.smartops.comun.errores;

import java.util.Map;

/**
 * Error de negocio con un código del catálogo. La capa de controladores lo convierte en una
 * respuesta {@code application/problem+json} (RFC 9457) con el estado HTTP del código. Las propiedades opcionales
 * se añaden al problema como extensiones, por ejemplo {@code clienteId} en {@code DOCUMENTO_CLIENTE_DUPLICADO}.
 */
public class ErrorDeNegocio extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final CodigoError codigo;
	private final Map<String, Object> propiedades;

	public ErrorDeNegocio(CodigoError codigo, String detalle) {
		this(codigo, detalle, Map.of());
	}

	public ErrorDeNegocio(CodigoError codigo, String detalle, Map<String, Object> propiedades) {
		super(detalle);
		this.codigo = codigo;
		this.propiedades = Map.copyOf(propiedades);
	}

	public CodigoError codigo() {
		return codigo;
	}

	/** Extensiones opcionales del problema, sin incluir los campos estándar. */
	public Map<String, Object> propiedades() {
		return propiedades;
	}
}
