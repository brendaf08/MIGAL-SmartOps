package com.migal.smartops.agenteia.service;

import static com.migal.smartops.agenteia.service.ParametrosHerramienta.LIMITE;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.entero;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.enumerado;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.fecha;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.requerido;

import com.migal.smartops.comercial.model.EstadoVenta;
import com.migal.smartops.comercial.model.Venta;
import com.migal.smartops.comercial.service.VentaService;
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
 * Herramientas del agente sobre las ventas (CU-05). Los roles son los declarados en x-roles de las operaciones
 * {@code listarVentas} y {@code consultarVenta}.
 */
@Component
public class HerramientasVentas implements ProveedorDeHerramientas {

	private static final Set<RolUsuario> ROLES = Set.of(RolUsuario.ALMACENERO, RolUsuario.VENDEDOR,
			RolUsuario.ADMINISTRADOR);

	private final VentaService ventas;

	public HerramientasVentas(VentaService ventas) {
		this.ventas = ventas;
	}

	@Override
	public List<DefinicionHerramienta> definiciones() {
		return List.of(
				new DefinicionHerramienta("listarVentas", "Listar ventas", "GET /ventas", ROLES, this::listar),
				new DefinicionHerramienta("consultarVenta", "Consultar una venta", "GET /ventas/{id}", ROLES,
						this::consultar));
	}

	private ResultadoHerramienta listar(Map<String, String> parametros) {
		EstadoVenta estado = enumerado(parametros, "estado", EstadoVenta::de);
		Page<Venta> pagina = ventas.listar(estado == null ? null : List.of(estado), entero(parametros, "clienteId"),
				fecha(parametros, "desde"), fecha(parametros, "hasta"),
				PageRequest.of(0, LIMITE, Sort.by(Sort.Direction.DESC, "fechaVenta")));
		List<ReferenciaAgente> referencias = pagina.getContent().stream()
				.map(venta -> new ReferenciaAgente(ReferenciaAgente.VENTA, String.valueOf(venta.getId()),
						venta.getCodigo()))
				.toList();
		if (pagina.getTotalElements() == 0) {
			return new ResultadoHerramienta("No hay ventas que coincidan con la consulta.", referencias);
		}
		String detalle = pagina.getContent().stream().map(this::resumen).collect(Collectors.joining(" "));
		return new ResultadoHerramienta("Hay " + pagina.getTotalElements() + " ventas. " + detalle, referencias);
	}

	private ResultadoHerramienta consultar(Map<String, String> parametros) {
		Venta venta = ventas.consultar(requerido(parametros, "id")).venta();
		return new ResultadoHerramienta(resumen(venta), List.of(new ReferenciaAgente(ReferenciaAgente.VENTA,
				String.valueOf(venta.getId()), venta.getCodigo())));
	}

	private String resumen(Venta venta) {
		return venta.getCodigo() + ": estado " + venta.getEstado().valor() + ", total " + venta.total().toPlainString()
				+ ".";
	}
}
