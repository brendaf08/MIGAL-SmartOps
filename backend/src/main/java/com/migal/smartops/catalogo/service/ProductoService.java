package com.migal.smartops.catalogo.service;

import com.migal.smartops.catalogo.model.Categoria;
import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.repository.ProductoRepository;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administración del catálogo maestro de productos (CU-03). El SKU es la clave natural y no se modifica tras
 * el registro. Los productos inactivos se conservan con su historial. La categoría debe existir: si no existe,
 * la operación responde {@code VALIDACION_FALLIDA}, según el contrato del módulo.
 */
@Service
public class ProductoService {

	/**
	 * Datos editables de un producto. El identificador de la categoría llega ya convertido a número.
	 *
	 * @param precioReferencial importe de referencia, que se redondea a dos decimales
	 * @param stockMinimo       umbral de alerta, que se redondea a tres decimales
	 */
	public record DatosProducto(String nombreOficial, String descripcion, String unidadMedida,
			BigDecimal precioReferencial, BigDecimal stockMinimo, long idCategoria, String marca,
			String presentacion) {
	}

	private final ProductoRepository productos;
	private final CategoriaService categorias;
	private final Clock reloj;

	public ProductoService(ProductoRepository productos, CategoriaService categorias, Clock reloj) {
		this.productos = productos;
		this.categorias = categorias;
		this.reloj = reloj;
	}

	/**
	 * Lista paginada de productos. Los filtros opcionales son: {@code q} (SKU o nombre, sin distinguir
	 * mayúsculas), {@code sku} (exacto), {@code idCategoria} y {@code estadoActivo}. Si la vigencia se omite,
	 * se devuelven productos activos e inactivos.
	 */
	@Transactional(readOnly = true)
	public Page<Producto> listar(String q, String sku, Long idCategoria, Boolean estadoActivo, Pageable paginacion) {
		Specification<Producto> filtros = (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			if (q != null && !q.isBlank()) {
				String patron = "%" + q.trim().toLowerCase() + "%";
				condiciones.add(cb.or(
						cb.like(cb.lower(raiz.get("sku")), patron),
						cb.like(cb.lower(raiz.get("nombreOficial")), patron)));
			}
			if (sku != null && !sku.isBlank()) {
				condiciones.add(cb.equal(raiz.get("sku"), sku.trim()));
			}
			if (idCategoria != null) {
				condiciones.add(cb.equal(raiz.get("categoria").get("id"), idCategoria));
			}
			if (estadoActivo != null) {
				condiciones.add(cb.equal(raiz.get("estadoActivo"), estadoActivo));
			}
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
		return productos.findAll(filtros, paginacion);
	}

	@Transactional(readOnly = true)
	public Producto consultar(long id) {
		return productos.findById(id).orElseThrow(ProductoService::noEncontrado);
	}

	/**
	 * Registra un producto nuevo sin existencias. Si el SKU ya existe, o si la base de datos rechaza la
	 * inserción por una restricción de unicidad, responde {@code SKU_DUPLICADO}.
	 */
	@Transactional
	public Producto registrar(String sku, DatosProducto datos, boolean estadoActivo) {
		validarImportes(datos);
		if (productos.existsBySku(sku)) {
			throw skuDuplicado();
		}
		Categoria categoria = categoriaVigente(datos.idCategoria());
		Producto nuevo = new Producto(sku, datos.nombreOficial(), datos.descripcion(), datos.unidadMedida(),
				importe(datos.precioReferencial()), cantidad(datos.stockMinimo()), categoria, datos.marca(),
				datos.presentacion(), estadoActivo);
		try {
			return productos.saveAndFlush(nuevo);
		} catch (DataIntegrityViolationException e) {
			throw skuDuplicado();
		}
	}

	/** Modifica los datos del producto. El SKU permanece sin cambios. */
	@Transactional
	public Producto actualizar(long id, DatosProducto datos) {
		Producto producto = consultar(id);
		validarImportes(datos);
		Categoria categoria = categoriaVigente(datos.idCategoria());
		producto.actualizar(datos.nombreOficial(), datos.descripcion(), datos.unidadMedida(),
				importe(datos.precioReferencial()), cantidad(datos.stockMinimo()), categoria, datos.marca(),
				datos.presentacion(), reloj.instant());
		return productos.save(producto);
	}

	/** Activa o desactiva la vigencia del producto. La desactivación es lógica. */
	@Transactional
	public Producto cambiarEstado(long id, boolean activo) {
		Producto producto = consultar(id);
		producto.cambiarEstado(activo, reloj.instant());
		return productos.save(producto);
	}

	private Categoria categoriaVigente(long idCategoria) {
		try {
			return categorias.validarExistencia(idCategoria);
		} catch (ErrorDeNegocio e) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "La categoría indicada no existe.");
		}
	}

	private static void validarImportes(DatosProducto datos) {
		if (datos.precioReferencial() == null || datos.precioReferencial().signum() < 0) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "El precio referencial no puede ser negativo.");
		}
		if (datos.stockMinimo() == null || datos.stockMinimo().signum() < 0) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "El stock mínimo no puede ser negativo.");
		}
	}

	private static BigDecimal importe(BigDecimal valor) {
		return valor.setScale(2, RoundingMode.HALF_UP);
	}

	private static BigDecimal cantidad(BigDecimal valor) {
		return valor.setScale(3, RoundingMode.HALF_UP);
	}

	private static ErrorDeNegocio skuDuplicado() {
		return new ErrorDeNegocio(CodigoError.SKU_DUPLICADO, "El SKU ya existe en el catálogo.");
	}

	private static ErrorDeNegocio noEncontrado() {
		return new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El producto no existe.");
	}
}
