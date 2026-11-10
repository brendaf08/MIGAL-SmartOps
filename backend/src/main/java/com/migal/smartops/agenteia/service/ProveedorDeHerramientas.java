package com.migal.smartops.agenteia.service;

import java.util.List;

/**
 * Fuente de herramientas del agente. Cada módulo que expone operaciones marcadas con {@code x-herramienta-agente} las
 * declara mediante una implementación de esta interfaz. {@link CatalogoHerramientas} reúne todas las implementaciones.
 */
public interface ProveedorDeHerramientas {

	/** Definiciones de las herramientas que aporta el módulo. */
	List<DefinicionHerramienta> definiciones();
}
