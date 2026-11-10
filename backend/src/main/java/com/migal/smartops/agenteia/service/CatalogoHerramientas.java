package com.migal.smartops.agenteia.service;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.seguridad.model.RolUsuario;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Catálogo de herramientas del agente (RF-IA-09). Solo ofrece y ejecuta las herramientas autorizadas para el rol del
 * usuario autenticado: una herramienta fuera de su rol no aparece en el catálogo del rol y, si se solicita, no se ejecuta
 * y se informa con PERMISO_INSUFICIENTE. Los servicios de negocio no declaran autorización por método, por lo que este
 * catálogo es el punto de control de las herramientas.
 */
@Component
public class CatalogoHerramientas {

	private final Map<String, DefinicionHerramienta> porNombre = new LinkedHashMap<>();

	public CatalogoHerramientas(List<ProveedorDeHerramientas> proveedores) {
		for (ProveedorDeHerramientas proveedor : proveedores) {
			for (DefinicionHerramienta definicion : proveedor.definiciones()) {
				if (porNombre.putIfAbsent(definicion.nombre(), definicion) != null) {
					throw new IllegalStateException("Herramienta duplicada en el agente: " + definicion.nombre());
				}
			}
		}
	}

	/** Herramientas que el rol puede usar, en el orden de registro. */
	public List<DefinicionHerramienta> disponiblesPara(RolUsuario rol) {
		return porNombre.values().stream().filter(definicion -> definicion.roles().contains(rol)).toList();
	}

	/**
	 * Ejecuta una herramienta con los permisos del rol. Una herramienta inexistente responde VALIDACION_FALLIDA; una que el
	 * rol no puede usar responde PERMISO_INSUFICIENTE sin ejecutarse. Un error de negocio del servicio se informa con su
	 * código y no interrumpe la respuesta del agente.
	 */
	public InvocacionHerramienta ejecutar(RolUsuario rol, String nombre, Map<String, String> parametros) {
		DefinicionHerramienta definicion = porNombre.get(nombre);
		if (definicion == null) {
			return InvocacionHerramienta.fallida(nombre, parametros, "La herramienta solicitada no existe.",
					CodigoError.VALIDACION_FALLIDA);
		}
		if (!definicion.roles().contains(rol)) {
			return InvocacionHerramienta.fallida(nombre, parametros,
					"La herramienta no está disponible para el rol del usuario.", CodigoError.PERMISO_INSUFICIENTE);
		}
		try {
			return InvocacionHerramienta.exitosa(nombre, parametros, definicion.ejecucion().apply(parametros));
		} catch (ErrorDeNegocio error) {
			return InvocacionHerramienta.fallida(nombre, parametros, error.getMessage(), error.codigo());
		}
	}
}
