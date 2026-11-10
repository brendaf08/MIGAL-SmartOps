package com.migal.smartops.agenteia.service;

import com.migal.smartops.seguridad.model.RolUsuario;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Herramienta del agente: una operación de solo lectura de otro módulo, con los roles que pueden usarla y la función que
 * la ejecuta. El nombre coincide con el operationId del contrato, y la operación es el método y la ruta relativos a
 * {@code /api/v1}. La función recibe los parámetros de la invocación y puede lanzar {@code ErrorDeNegocio}.
 *
 * @param nombre      operationId de la operación del contrato
 * @param descripcion descripción breve de la consulta
 * @param operacion   método y ruta de la operación, por ejemplo {@code GET /inventario/alertas}
 * @param roles       roles autorizados a usar la herramienta, según x-roles del contrato
 * @param ejecucion   función que consulta el servicio de negocio y devuelve el resultado
 */
public record DefinicionHerramienta(String nombre, String descripcion, String operacion, Set<RolUsuario> roles,
		Function<Map<String, String>, ResultadoHerramienta> ejecucion) {

	public DefinicionHerramienta {
		roles = Set.copyOf(roles);
	}
}
