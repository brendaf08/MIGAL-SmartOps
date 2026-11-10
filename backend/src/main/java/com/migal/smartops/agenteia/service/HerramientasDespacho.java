package com.migal.smartops.agenteia.service;

import static com.migal.smartops.agenteia.service.ParametrosHerramienta.LIMITE;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.entero;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.enumerado;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.fecha;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.requerido;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.texto;

import com.migal.smartops.comercial.model.Venta;
import com.migal.smartops.despacho.model.EstadoGuia;
import com.migal.smartops.despacho.model.GuiaDespacho;
import com.migal.smartops.despacho.service.DespachoService;
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
 * Herramientas del agente sobre los despachos (CU-06). La operación {@code obtenerDocumentoGuia} devuelve la misma
 * representación de la guía que {@code consultarGuiaDespacho}, por lo que la herramienta consulta el mismo servicio.
 * Los roles son los declarados en x-roles de cada operación.
 */
@Component
public class HerramientasDespacho implements ProveedorDeHerramientas {

	private static final Set<RolUsuario> ROLES = Set.of(RolUsuario.ALMACENERO, RolUsuario.ADMINISTRADOR);

	private final DespachoService despacho;

	public HerramientasDespacho(DespachoService despacho) {
		this.despacho = despacho;
	}

	@Override
	public List<DefinicionHerramienta> definiciones() {
		return List.of(
				new DefinicionHerramienta("listarGuiasDespacho", "Listar guías de despacho", "GET /guias-despacho",
						ROLES, this::listarGuias),
				new DefinicionHerramienta("consultarGuiaDespacho", "Consultar una guía de despacho",
						"GET /guias-despacho/{id}", ROLES, this::consultarGuia),
				new DefinicionHerramienta("obtenerDocumentoGuia", "Obtener el documento de una guía de despacho",
						"GET /guias-despacho/{id}/documento", ROLES, this::consultarGuia),
				new DefinicionHerramienta("listarVentasPendientesDespacho",
						"Listar ventas pendientes de despacho", "GET /ventas-pendientes-despacho", ROLES,
						this::ventasPendientes));
	}

	private ResultadoHerramienta listarGuias(Map<String, String> parametros) {
		EstadoGuia estado = enumerado(parametros, "estado", EstadoGuia::de);
		Page<GuiaDespacho> pagina = despacho.listar(estado == null ? null : List.of(estado), texto(parametros, "q"),
				entero(parametros, "idVenta"), fecha(parametros, "desde"), fecha(parametros, "hasta"),
				PageRequest.of(0, LIMITE, Sort.by(Sort.Direction.DESC, "fechaEmision")));
		List<ReferenciaAgente> referencias = pagina.getContent().stream()
				.map(guia -> new ReferenciaAgente(ReferenciaAgente.GUIA_DESPACHO, String.valueOf(guia.getId()),
						guia.getCodigo()))
				.toList();
		if (pagina.getTotalElements() == 0) {
			return new ResultadoHerramienta("No hay guías de despacho que coincidan con la consulta.", referencias);
		}
		String detalle = pagina.getContent().stream().map(guia -> resumen(guia.getCodigo(), guia.getEstado().valor(),
				guia.getVenta())).collect(Collectors.joining(" "));
		return new ResultadoHerramienta("Hay " + pagina.getTotalElements() + " guías de despacho. " + detalle,
				referencias);
	}

	private ResultadoHerramienta consultarGuia(Map<String, String> parametros) {
		GuiaDespacho guia = despacho.consultar(requerido(parametros, "id")).guia();
		return new ResultadoHerramienta(resumen(guia.getCodigo(), guia.getEstado().valor(), guia.getVenta()),
				List.of(new ReferenciaAgente(ReferenciaAgente.GUIA_DESPACHO, String.valueOf(guia.getId()),
						guia.getCodigo())));
	}

	private ResultadoHerramienta ventasPendientes(Map<String, String> parametros) {
		Page<Venta> pagina = despacho.ventasPendientes(texto(parametros, "q"), fecha(parametros, "desde"),
				fecha(parametros, "hasta"), PageRequest.of(0, LIMITE, Sort.by(Sort.Direction.ASC, "fechaVenta")));
		List<ReferenciaAgente> referencias = pagina.getContent().stream()
				.map(venta -> new ReferenciaAgente(ReferenciaAgente.VENTA, String.valueOf(venta.getId()),
						venta.getCodigo()))
				.toList();
		if (pagina.getTotalElements() == 0) {
			return new ResultadoHerramienta("No hay ventas pendientes de despacho.", referencias);
		}
		String detalle = pagina.getContent().stream().map(venta -> venta.getCodigo() + ", total "
				+ venta.total().toPlainString() + ".").collect(Collectors.joining(" "));
		return new ResultadoHerramienta("Hay " + pagina.getTotalElements() + " ventas pendientes de despacho. "
				+ detalle, referencias);
	}

	private static String resumen(String codigo, String estado, Venta venta) {
		return "Guía " + codigo + ": estado " + estado + ", venta " + venta.getCodigo() + ".";
	}
}
