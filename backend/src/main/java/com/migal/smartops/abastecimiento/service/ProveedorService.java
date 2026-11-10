package com.migal.smartops.abastecimiento.service;

import com.migal.smartops.abastecimiento.model.CatalogoProveedor;
import com.migal.smartops.abastecimiento.model.Proveedor;
import com.migal.smartops.abastecimiento.repository.CatalogoProveedorRepository;
import com.migal.smartops.abastecimiento.repository.ProveedorRepository;
import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.repository.ProductoRepository;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.comun.web.RespuestaProblema.ErrorCampo;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Directorio de proveedores y de su catálogo de productos (CU-11). El RUC es único, la desactivación es lógica y el
 * catálogo se reemplaza por completo en cada actualización. Un proveedor inactivo conserva su historial.
 */
@Service
public class ProveedorService {

	/** Datos de un proveedor registrado o modificado. */
	public record DatosProveedor(String ruc, String razonSocial, String contacto, String condiciones) {
	}

	/** Línea del catálogo enviada en una actualización, identificada por su SKU. */
	public record EntradaCatalogo(String sku, BigDecimal precioCompra, Integer plazoEntregaReferencialDias) {
	}

	private final ProveedorRepository proveedores;
	private final CatalogoProveedorRepository catalogos;
	private final ProductoRepository productos;
	private final Clock reloj;

	public ProveedorService(ProveedorRepository proveedores, CatalogoProveedorRepository catalogos,
			ProductoRepository productos, Clock reloj) {
		this.proveedores = proveedores;
		this.catalogos = catalogos;
		this.productos = productos;
		this.reloj = reloj;
	}

	/**
	 * Lista paginada de proveedores. {@code texto} busca por razón social o RUC, {@code estadoActivo} filtra por vigencia
	 * y {@code sku} devuelve los proveedores que suministran el producto indicado.
	 */
	@Transactional(readOnly = true)
	public Page<Proveedor> listar(String texto, Boolean estadoActivo, String sku, Pageable paginacion) {
		String patron = texto == null || texto.isBlank() ? null : "%" + texto.trim().toLowerCase() + "%";
		List<Long> proveedoresSku = sku == null || sku.isBlank() ? null : catalogos.idsProveedoresPorSku(sku.trim());
		Specification<Proveedor> filtros = (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			if (patron != null) {
				condiciones.add(cb.or(cb.like(cb.lower(raiz.get("razonSocial")), patron),
						cb.like(cb.lower(raiz.get("ruc")), patron)));
			}
			if (estadoActivo != null) {
				condiciones.add(cb.equal(raiz.get("estadoActivo"), estadoActivo));
			}
			if (proveedoresSku != null) {
				condiciones.add(proveedoresSku.isEmpty() ? cb.disjunction() : raiz.get("id").in(proveedoresSku));
			}
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
		return proveedores.findAll(filtros, paginacion);
	}

	/** Consulta un proveedor, activo o inactivo. */
	@Transactional(readOnly = true)
	public Proveedor consultar(long id) {
		return proveedores.findById(id).orElseThrow(ProveedorService::noEncontrado);
	}

	/** Registra un proveedor activo o inactivo según el indicador. El RUC no puede pertenecer a otro proveedor. */
	@Transactional
	public Proveedor registrar(DatosProveedor datos, boolean estadoActivo) {
		verificarRucLibre(datos.ruc(), null);
		return guardar(new Proveedor(datos.ruc(), datos.razonSocial(), datos.contacto(), datos.condiciones(), estadoActivo));
	}

	/** Modifica los datos de un proveedor. La vigencia se cambia con {@link #cambiarEstado(long, boolean)}. */
	@Transactional
	public Proveedor actualizar(long id, DatosProveedor datos) {
		Proveedor proveedor = proveedores.findById(id).orElseThrow(ProveedorService::noEncontrado);
		verificarRucLibre(datos.ruc(), id);
		proveedor.actualizar(datos.ruc(), datos.razonSocial(), datos.contacto(), datos.condiciones());
		return guardar(proveedor);
	}

	/** Activa o desactiva el proveedor. La desactivación no elimina su historial. */
	@Transactional
	public Proveedor cambiarEstado(long id, boolean activo) {
		Proveedor proveedor = proveedores.findById(id).orElseThrow(ProveedorService::noEncontrado);
		proveedor.cambiarEstado(activo);
		return guardar(proveedor);
	}

	/** Catálogo completo del proveedor, en orden ascendente de producto. */
	@Transactional(readOnly = true)
	public List<CatalogoProveedor> catalogo(long id) {
		consultar(id);
		return catalogos.findByIdIdProveedorOrderByIdIdProductoAsc(id);
	}

	/**
	 * Reemplaza el catálogo del proveedor. Cada SKU debe existir en el catálogo maestro y no puede repetirse; si no se
	 * cumple, responde VALIDACION_FALLIDA con la lista de errores y no modifica el catálogo. Los productos omitidos dejan
	 * de asociarse al proveedor. Devuelve el catálogo resultante.
	 */
	@Transactional
	public List<CatalogoProveedor> actualizarCatalogo(long id, List<EntradaCatalogo> entradas) {
		Proveedor proveedor = proveedores.findById(id).orElseThrow(ProveedorService::noEncontrado);
		Map<String, Producto> porSku = resolverProductos(entradas);
		Map<Long, CatalogoProveedor> existentes = new HashMap<>();
		catalogos.findByIdIdProveedorOrderByIdIdProductoAsc(id)
				.forEach(c -> existentes.put(c.getProducto().getId(), c));

		Instant ahora = reloj.instant();
		List<CatalogoProveedor> nuevos = new ArrayList<>();
		for (EntradaCatalogo entrada : entradas) {
			Producto producto = porSku.get(entrada.sku());
			CatalogoProveedor actual = existentes.remove(producto.getId());
			if (actual == null) {
				nuevos.add(new CatalogoProveedor(proveedor, producto, entrada.precioCompra(),
						entrada.plazoEntregaReferencialDias(), ahora));
			} else {
				actual.actualizar(entrada.precioCompra(), entrada.plazoEntregaReferencialDias(), ahora);
			}
		}
		// Las asociaciones que quedan en el mapa corresponden a productos omitidos y se eliminan antes de insertar.
		catalogos.deleteAll(existentes.values());
		catalogos.flush();
		catalogos.saveAllAndFlush(nuevos);
		return catalogos.findByIdIdProveedorOrderByIdIdProductoAsc(id);
	}

	private void verificarRucLibre(String ruc, Long idActual) {
		proveedores.findByRuc(ruc).filter(p -> !p.getId().equals(idActual)).ifPresent(p -> {
			throw new ErrorDeNegocio(CodigoError.RUC_DUPLICADO, "El RUC ya pertenece a otro proveedor.",
					Map.of("proveedorId", p.getId().toString()));
		});
	}

	private Proveedor guardar(Proveedor proveedor) {
		try {
			return proveedores.saveAndFlush(proveedor);
		} catch (DataIntegrityViolationException e) {
			throw new ErrorDeNegocio(CodigoError.RUC_DUPLICADO, "El RUC ya pertenece a otro proveedor.");
		}
	}

	/**
	 * Resuelve los SKU de un catálogo contra el catálogo maestro y devuelve los productos indexados por SKU. Un SKU
	 * repetido o inexistente se reporta en la lista de errores del problema.
	 */
	private Map<String, Producto> resolverProductos(List<EntradaCatalogo> entradas) {
		List<ErrorCampo> errores = new ArrayList<>();
		Set<String> vistos = new HashSet<>();
		Map<String, Producto> porSku = new HashMap<>();
		for (int i = 0; i < entradas.size(); i++) {
			String sku = entradas.get(i).sku();
			String campo = "productos[" + i + "].sku";
			if (!vistos.add(sku)) {
				errores.add(new ErrorCampo(campo, "SKU_DUPLICADO", "El SKU se repite en el catálogo."));
				continue;
			}
			Optional<Producto> producto = productos.findBySku(sku);
			if (producto.isEmpty()) {
				errores.add(new ErrorCampo(campo, "RECURSO_NO_ENCONTRADO", "El SKU no existe en el catálogo maestro."));
			} else {
				porSku.put(sku, producto.get());
			}
		}
		if (!errores.isEmpty()) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "El catálogo del proveedor contiene errores.",
					Map.of("errores", errores));
		}
		return porSku;
	}

	private static ErrorDeNegocio noEncontrado() {
		return new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El proveedor no existe.");
	}
}
