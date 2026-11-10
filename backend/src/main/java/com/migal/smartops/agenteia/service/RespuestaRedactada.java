package com.migal.smartops.agenteia.service;

/**
 * Respuesta en lenguaje natural redactada por el proveedor del modelo.
 *
 * @param texto    respuesta en español; cuando {@code incierta} es verdadero, no incluye cantidades ni ubicaciones inventadas
 * @param incierta verdadero cuando la información disponible no basta para responder (RF-IA-08)
 */
public record RespuestaRedactada(String texto, boolean incierta) {
}
