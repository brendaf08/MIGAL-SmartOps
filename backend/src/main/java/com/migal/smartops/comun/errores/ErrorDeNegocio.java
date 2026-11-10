package com.migal.smartops.comun.errores;

/**
 * Error de negocio con un código del catálogo. La capa de controladores lo convierte en una
 * respuesta {@code application/problem+json} (RFC 9457) con el estado HTTP del código.
 */
public class ErrorDeNegocio extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final CodigoError codigo;

	public ErrorDeNegocio(CodigoError codigo, String detalle) {
		super(detalle);
		this.codigo = codigo;
	}

	public CodigoError codigo() {
		return codigo;
	}
}
