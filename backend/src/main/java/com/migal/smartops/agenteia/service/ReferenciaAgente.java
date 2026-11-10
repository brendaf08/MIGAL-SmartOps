package com.migal.smartops.agenteia.service;

/**
 * Registro de negocio consultado por una herramienta, según el esquema {@code AgenteReferencia} del contrato. El tipo
 * toma uno de los valores {@link #PRODUCTO}, {@link #ALMACEN}, {@link #VENTA}, {@link #GUIA_DESPACHO} o
 * {@link #MOVIMIENTO}. El código es nulo cuando el registro no tiene un código de negocio visible.
 *
 * @param tipo   tipo del registro consultado
 * @param id     identificador técnico del registro
 * @param codigo código de negocio visible, o nulo
 */
public record ReferenciaAgente(String tipo, String id, String codigo) {

	public static final String PRODUCTO = "producto";
	public static final String ALMACEN = "almacen";
	public static final String VENTA = "venta";
	public static final String GUIA_DESPACHO = "guia_despacho";
	public static final String MOVIMIENTO = "movimiento";
}
