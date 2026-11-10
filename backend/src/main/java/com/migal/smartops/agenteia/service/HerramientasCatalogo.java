package com.migal.smartops.agenteia.service;

import static com.migal.smartops.agenteia.service.ParametrosHerramienta.LIMITE;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.entero;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.requerido;
import static com.migal.smartops.agenteia.service.ParametrosHerramienta.texto;

import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.service.ProductoService;
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
 * Herramientas del agente sobre el catálogo de productos. Los roles son los declarados en x-roles de las operaciones
 * {@code listarProductos} y {@code consultarProducto}.
 */
@Component
public class HerramientasCatalogo implements ProveedorDeHerramientas {

	private static final Set<RolUsuario> ROLES = Set.of(RolUsuario.ALMACENERO, RolUsuario.ADMINISTRADOR);

	private final ProductoService productos;

	public HerramientasCatalogo(ProductoService productos) {
		this.productos = productos;
	}

	@Override
	public List<DefinicionHerramienta> definiciones() {
		return List.of(
				new DefinicionHerramienta("listarProductos", "Listar productos del catálogo", "GET /productos", ROLES,
						this::listar),
				new DefinicionHerramienta("consultarProducto", "Consultar un producto del catálogo",
						"GET /productos/{id}", ROLES, this::consultar));
	}

	private ResultadoHerramienta listar(Map<String, String> parametros) {
		String estado = texto(parametros, "estadoActivo");
		Page<Producto> pagina = productos.listar(texto(parametros, "q"), texto(parametros, "sku"),
				entero(parametros, "idCategoria"), estado == null ? null : Boolean.valueOf(estado),
				PageRequest.of(0, LIMITE, Sort.by("sku")));
		List<ReferenciaAgente> referencias = pagina.getContent().stream()
				.map(producto -> new ReferenciaAgente(ReferenciaAgente.PRODUCTO, String.valueOf(producto.getId()),
						producto.getSku()))
				.toList();
		if (pagina.getTotalElements() == 0) {
			return new ResultadoHerramienta("No hay productos que coincidan con la consulta.", referencias);
		}
		String detalle = pagina.getContent().stream().map(producto -> producto.getSku() + " "
				+ producto.getNombreOficial() + " (" + producto.getUnidadMedida() + ").")
				.collect(Collectors.joining(" "));
		return new ResultadoHerramienta("Hay " + pagina.getTotalElements() + " productos. " + detalle, referencias);
	}

	private ResultadoHerramienta consultar(Map<String, String> parametros) {
		Producto producto = productos.consultar(requerido(parametros, "id"));
		String resumen = producto.getSku() + ": " + producto.getNombreOficial() + ". Unidad de medida: "
				+ producto.getUnidadMedida() + ". Stock mínimo: " + producto.getStockMinimo().toPlainString() + ".";
		return new ResultadoHerramienta(resumen, List.of(new ReferenciaAgente(ReferenciaAgente.PRODUCTO,
				String.valueOf(producto.getId()), producto.getSku())));
	}
}
