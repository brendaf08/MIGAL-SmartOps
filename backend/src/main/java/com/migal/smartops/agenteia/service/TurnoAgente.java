package com.migal.smartops.agenteia.service;

/** Mensaje previo de la conversación que se envía al modelo como contexto: del usuario o del agente. */
public record TurnoAgente(boolean delUsuario, String texto) {
}
