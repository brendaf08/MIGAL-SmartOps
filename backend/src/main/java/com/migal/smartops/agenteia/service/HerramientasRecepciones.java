package com.migal.smartops.agenteia.service;

import static com.migal.smartops.agenteia.service.ParametrosHerramienta.LIMITE;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.entero;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.enumerado;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.fecha;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.requerido;

import com.migal.smartops.recepciones.model.EstadoRecepcion;
import com.migal.smartops.recepciones.model.Recepcion;
import com.migal.smartops.recepciones.service.RecepcionService;
import com.migal.smartops.recepciones.service.RecepcionService.RecepcionResumen;
import com.migal.smartops.seguridad.model.RolUsuario;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Herramientas del agente sobre las recepciones de mercadería (CU-09). Los roles son los declarados en x-roles de las
 * operaciones {@code listarRecepciones} y {@code consultarRecepcion}. El contrato no define un tipo de referencia para
 * las recepciones, por lo que las respuestas no incluyen referencias.
 */
@Component
public class HerramientasRecepciones implements ProveedorDeHerramientas {

	private static final Set<RolUsuario> ROLES = Set.of(RolUsuario.ALMACENERO, RolUsuario.ADMINISTRADOR);

	private final RecepcionService recepciones;

	public HerramientasRecepciones(RecepcionService recepciones) {
		this.recepciones = recepciones;
	}

	@Override
	public List<DefinicionHerramienta> definiciones() {
		return List.of(
				new DefinicionHerramienta("listarRecepciones", "Listar recepciones de mercadería", "GET /recepciones",
						ROLES, this::listar),
				new DefinicionHerramienta("consultarRecepcion", "Consultar una recepción de mercadería",
						"GET /recepciones/{id}", ROLES, this::consultar));
	}

	private ResultadoHerramienta listar(Map<String, String> parametros) {
		EstadoRecepcion estado = enumerado(parametros, "estado", EstadoRecepcion::de);
		Page<RecepcionResumen> pagina = recepciones.listar(entero(parametros, "ordenCompraId"),
				estado == null ? null : List.of(estado), fecha(parametros, "desde"), fecha(parametros, "hasta"),
				PageRequest.of(0, LIMITE, Sort.by(Sort.Direction.DESC, "fechaRecepcion")));
		if (pagina.getTotalElements() == 0) {
			return new ResultadoHerramienta("No hay recepciones que coincidan con la consulta.", List.of());
		}
		String detalle = pagina.getContent().stream().map(recepcion -> recepcion.codigo() + " de la orden "
				+ recepcion.codigoOrdenCompra() + ", proveedor " + recepcion.proveedor() + ", estado "
				+ recepcion.estado().valor() + ".").collect(Collectors.joining(" "));
		return new ResultadoHerramienta("Hay " + pagina.getTotalElements() + " recepciones. " + detalle, List.of());
	}

	private ResultadoHerramienta consultar(Map<String, String> parametros) {
		Recepcion recepcion = recepciones.consultar(requerido(parametros, "id")).recepcion();
		return new ResultadoHerramienta("Recepción " + recepcion.getCodigo() + ": estado "
				+ recepcion.getEstado().valor() + ", fecha " + recepcion.getFechaRecepcion() + ".", List.of());
	}
}
