package com.migal.smartops.agenteia.service;

import static com.migal.smartops.agenteia.service.ParametrosHerramienta.LIMITE;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.booleano;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.entero;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.fecha;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.requerido;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.texto;

import com.migal.smartops.agenteia.service.ReferenciaAgente;
import com.migal.smartops.inventario.service.ConsultaInventarioService;
import com.migal.smartops.inventario.service.ConsultaInventarioService.AlertaStock;
import com.migal.smartops.inventario.service.ConsultaInventarioService.ProductoExistencias;
import com.migal.smartops.inventario.service.MovimientoDetalle;
import com.migal.smartops.seguridad.model.RolUsuario;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Herramientas del agente sobre el módulo de inventario (CU-04 y CU-07). Cada una invoca el servicio de consultas con
 * los parámetros del contrato y resume el primer lote de resultados. Los roles son los declarados en x-roles de cada
 * operación; el control de acceso lo aplica {@link CatalogoHerramientas}.
 */
@Component
public class HerramientasInventario implements ProveedorDeHerramientas {

	private static final Set<RolUsuario> CONSULTA_GENERAL = Set.of(RolUsuario.ALMACENERO, RolUsuario.VENDEDOR,
			RolUsuario.REPONEDOR, RolUsuario.ADMINISTRADOR);
	private static final Set<RolUsuario> ALERTAS = Set.of(RolUsuario.ALMACENERO, RolUsuario.REPONEDOR,
			RolUsuario.ADMINISTRADOR);
	private static final Set<RolUsuario> KARDEX = Set.of(RolUsuario.ALMACENERO, RolUsuario.ADMINISTRADOR);

	private final ConsultaInventarioService consultas;

	public HerramientasInventario(ConsultaInventarioService consultas) {
		this.consultas = consultas;
	}

	@Override
	public List<DefinicionHerramienta> definiciones() {
		return List.of(
				new DefinicionHerramienta("consultarDisponibilidad", "Consultar disponibilidad de productos",
						"GET /inventario/disponibilidad", CONSULTA_GENERAL, this::disponibilidad),
				new DefinicionHerramienta("consultarExistenciasProducto", "Consultar existencias de un producto",
						"GET /productos/{id}/existencias", CONSULTA_GENERAL, this::existencias),
				new DefinicionHerramienta("listarAlertasStock", "Listar productos en alerta de stock",
						"GET /inventario/alertas", ALERTAS, this::alertas),
				new DefinicionHerramienta("consultarKardex", "Consultar movimientos de Kardex",
						"GET /inventario/movimientos", KARDEX, this::kardex));
	}

	private ResultadoHerramienta disponibilidad(Map<String, String> parametros) {
		ConsultaInventarioService.Disponibilidad disponibilidad = consultas.disponibilidad(texto(parametros, "q"),
				texto(parametros, "sku"), entero(parametros, "categoriaId"), booleano(parametros, "soloAlerta"), 1,
				LIMITE);
		Page<ProductoExistencias> pagina = disponibilidad.pagina();
		List<ReferenciaAgente> referencias = pagina.getContent().stream()
				.map(producto -> new ReferenciaAgente(ReferenciaAgente.PRODUCTO, String.valueOf(producto.productoId()),
						producto.sku()))
				.toList();
		if (pagina.getTotalElements() == 0) {
			return new ResultadoHerramienta("No hay productos que coincidan con la consulta.", referencias);
		}
		String detalle = pagina.getContent().stream().map(producto -> producto.nombre() + " (" + producto.sku()
				+ "): libre " + producto.libre().toPlainString() + " " + producto.unidad() + ", nivel "
				+ producto.nivelAlerta() + ".").collect(Collectors.joining(" "));
		return new ResultadoHerramienta("Coinciden " + pagina.getTotalElements() + " productos. En alerta en total: "
				+ disponibilidad.enAlerta() + ". " + detalle, referencias);
	}

	private ResultadoHerramienta existencias(Map<String, String> parametros) {
		ProductoExistencias producto = consultas.existencias(requerido(parametros, "id"));
		String resumen = "Existencias de " + producto.nombre() + " (" + producto.sku() + "): física "
				+ producto.fisico().toPlainString() + ", reservada " + producto.reservado().toPlainString()
				+ ", libre " + producto.libre().toPlainString() + " " + producto.unidad() + ", nivel "
				+ producto.nivelAlerta() + ".";
		return new ResultadoHerramienta(resumen, List.of(new ReferenciaAgente(ReferenciaAgente.PRODUCTO,
				String.valueOf(producto.productoId()), producto.sku())));
	}

	private ResultadoHerramienta alertas(Map<String, String> parametros) {
		Page<AlertaStock> pagina = consultas.alertas(texto(parametros, "nivel"), 1, LIMITE);
		List<ReferenciaAgente> referencias = pagina.getContent().stream()
				.map(alerta -> new ReferenciaAgente(ReferenciaAgente.PRODUCTO, String.valueOf(alerta.productoId()),
						alerta.sku()))
				.toList();
		if (pagina.getTotalElements() == 0) {
			return new ResultadoHerramienta("No hay productos en alerta de stock.", referencias);
		}
		String detalle = pagina.getContent().stream().map(alerta -> alerta.nombre() + " (" + alerta.sku()
				+ "): faltante " + alerta.faltante().toPlainString() + " " + alerta.unidad() + ", nivel "
				+ alerta.nivelAlerta() + ".").collect(Collectors.joining(" "));
		return new ResultadoHerramienta("Hay " + pagina.getTotalElements() + " productos en alerta de stock. " + detalle,
				referencias);
	}

	private ResultadoHerramienta kardex(Map<String, String> parametros) {
		Page<MovimientoDetalle> pagina = consultas.kardex(texto(parametros, "q"), texto(parametros, "sku"),
				entero(parametros, "ubicacionId"), texto(parametros, "tipoOperacion"), texto(parametros, "origen"),
				fecha(parametros, "desde"), fecha(parametros, "hasta"), 1, LIMITE);
		List<ReferenciaAgente> referencias = pagina.getContent().stream()
				.map(movimiento -> new ReferenciaAgente(ReferenciaAgente.MOVIMIENTO, String.valueOf(movimiento.id()),
						null))
				.toList();
		if (pagina.getTotalElements() == 0) {
			return new ResultadoHerramienta("No hay movimientos de Kardex que coincidan con la consulta.", referencias);
		}
		String detalle = pagina.getContent().stream().map(movimiento -> movimiento.sku() + ": "
				+ movimiento.tipoOperacion().name() + " " + movimiento.cantidad().toPlainString() + " el "
				+ movimiento.fechaHora() + ".").collect(Collectors.joining(" "));
		return new ResultadoHerramienta("Hay " + pagina.getTotalElements() + " movimientos de Kardex. " + detalle,
				referencias);
	}
}
