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
import java.math.BigDecimal;
import java.util.LinkedHashMap;
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

	/** Con más coincidencias que estas no se calculan totales: la consulta es demasiado amplia. */
	private static final int MAXIMO_PARA_TOTALES = 200;

	private final ConsultaInventarioService consultas;

	public HerramientasInventario(ConsultaInventarioService consultas) {
		this.consultas = consultas;
	}

	@Override
	public List<DefinicionHerramienta> definiciones() {
		return List.of(
				new DefinicionHerramienta("consultarDisponibilidad", "Consultar disponibilidad de productos",
						"GET /inventario/disponibilidad", CONSULTA_GENERAL, List.of(
								ParametroHerramienta.texto("q", "Texto libre a buscar en el SKU o el nombre del producto."),
								ParametroHerramienta.texto("sku", "SKU exacto del producto."),
								ParametroHerramienta.entero("categoriaId", "Id técnico de la categoría del catálogo."),
								ParametroHerramienta.booleano("soloAlerta",
										"Si es true, solo productos en nivel mínimo o crítico.")),
						this::disponibilidad),
				new DefinicionHerramienta("consultarExistenciasProducto", "Consultar existencias de un producto",
						"GET /productos/{id}/existencias", CONSULTA_GENERAL,
						List.of(ParametroHerramienta.identificador("Id técnico del producto; no es el SKU.")),
						this::existencias),
				new DefinicionHerramienta("listarAlertasStock", "Listar productos en alerta de stock",
						"GET /inventario/alertas", ALERTAS,
						List.of(ParametroHerramienta.enumerado("nivel", "Nivel de alerta; sin valor incluye ambos.",
								"critico", "minimo")),
						this::alertas),
				new DefinicionHerramienta("consultarKardex", "Consultar movimientos de Kardex",
						"GET /inventario/movimientos", KARDEX, ParametroHerramienta.conRangoDeFechas(
								ParametroHerramienta.texto("q", "Texto libre a buscar en los movimientos."),
								ParametroHerramienta.texto("sku", "SKU exacto del producto."),
								ParametroHerramienta.entero("ubicacionId", "Id técnico de la tienda o el almacén."),
								ParametroHerramienta.enumerado("tipoOperacion", "Tipo de movimiento.", "entrada",
										"salida"),
								ParametroHerramienta.enumerado("origen", "Operación que originó el movimiento.",
										"despacho", "recepcion", "ajuste", "traslado")),
						this::kardex));
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
		StringBuilder resumen = new StringBuilder("Coinciden ").append(pagina.getTotalElements()).append(" productos (")
				.append(disponibilidad.enAlerta()).append(" en alerta en todo el catálogo).");
		String totales = totalesPorUnidad(parametros, pagina.getTotalElements());
		if (!totales.isEmpty()) {
			resumen.append("\nTotal de los productos encontrados:").append(totales);
		}
		resumen.append("\nDetalle:");
		pagina.getContent().forEach(producto -> resumen.append("\n• ").append(producto.nombre()).append(" (")
				.append(producto.sku()).append("): ").append(cantidad(producto.libre())).append(' ')
				.append(producto.unidad()).append(" libres, nivel ").append(producto.nivelAlerta())
				.append(producto.planificado().signum() > 0
						? ", por recibir " + cantidad(producto.planificado()) + " en órdenes de compra"
						: ""));
		if (pagina.getTotalElements() > pagina.getNumberOfElements()) {
			resumen.append("\n… y ").append(pagina.getTotalElements() - pagina.getNumberOfElements())
					.append(" productos más; precise el nombre para verlos.");
		}
		return new ResultadoHerramienta(resumen.toString(), referencias);
	}

	/** Suma física, reservada y libre de todas las coincidencias, agrupadas por unidad; vacío si son demasiadas. */
	private String totalesPorUnidad(Map<String, String> parametros, long coincidencias) {
		if (coincidencias > MAXIMO_PARA_TOTALES) {
			return "";
		}
		List<ProductoExistencias> todos = consultas.disponibilidad(texto(parametros, "q"), texto(parametros, "sku"),
				entero(parametros, "categoriaId"), booleano(parametros, "soloAlerta"), 1, MAXIMO_PARA_TOTALES)
				.pagina().getContent();
		Map<String, BigDecimal[]> porUnidad = new LinkedHashMap<>();
		for (ProductoExistencias producto : todos) {
			BigDecimal[] suma = porUnidad.computeIfAbsent(producto.unidad(),
					unidad -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO });
			suma[0] = suma[0].add(producto.fisico());
			suma[1] = suma[1].add(producto.reservado());
			suma[2] = suma[2].add(producto.libre());
			suma[3] = suma[3].add(producto.planificado());
		}
		StringBuilder texto = new StringBuilder();
		porUnidad.forEach((unidad, suma) -> texto.append("\n• ").append(unidad).append(": físico ")
				.append(cantidad(suma[0])).append(", reservado ").append(cantidad(suma[1])).append(", libre ")
				.append(cantidad(suma[2])).append(", por recibir (órdenes de compra) ").append(cantidad(suma[3])));
		return texto.toString();
	}

	/** Cantidad sin ceros decimales sobrantes. */
	private static String cantidad(BigDecimal valor) {
		BigDecimal limpio = valor.stripTrailingZeros();
		return (limpio.scale() < 0 ? limpio.setScale(0) : limpio).toPlainString();
	}

	private ResultadoHerramienta existencias(Map<String, String> parametros) {
		ProductoExistencias producto = consultas.existencias(requerido(parametros, "id"));
		String resumen = "Existencias de " + producto.nombre() + " (" + producto.sku() + "): física "
				+ producto.fisico().toPlainString() + ", reservada " + producto.reservado().toPlainString()
				+ ", libre " + producto.libre().toPlainString() + " " + producto.unidad() + ", por recibir en órdenes de compra "
				+ producto.planificado().toPlainString() + ", nivel " + producto.nivelAlerta() + ".";
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
		StringBuilder resumen = new StringBuilder("Hay ").append(pagina.getTotalElements())
				.append(" productos en alerta de stock. Los más graves:");
		pagina.getContent().forEach(alerta -> resumen.append("\n• ").append(alerta.nombre()).append(" (")
				.append(alerta.sku()).append("): faltan ").append(cantidad(alerta.faltante())).append(' ')
				.append(alerta.unidad()).append(", nivel ").append(alerta.nivelAlerta()));
		if (pagina.getTotalElements() > pagina.getNumberOfElements()) {
			resumen.append("\n… y ").append(pagina.getTotalElements() - pagina.getNumberOfElements())
					.append(" más; consulte la pantalla de alertas para verlos todos.");
		}
		return new ResultadoHerramienta(resumen.toString(), referencias);
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
