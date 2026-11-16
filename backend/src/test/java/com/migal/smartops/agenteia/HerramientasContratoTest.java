package com.migal.smartops.agenteia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.migal.smartops.agenteia.service.DefinicionHerramienta;
import com.migal.smartops.agenteia.service.HerramientasCatalogo;
import com.migal.smartops.agenteia.service.HerramientasDespacho;
import com.migal.smartops.agenteia.service.HerramientasInventario;
import com.migal.smartops.agenteia.service.HerramientasRecepciones;
import com.migal.smartops.agenteia.service.HerramientasVentas;
import com.migal.smartops.agenteia.service.ParametroHerramienta;
import com.migal.smartops.catalogo.service.ProductoService;
import com.migal.smartops.comercial.service.VentaService;
import com.migal.smartops.despacho.service.DespachoService;
import com.migal.smartops.inventario.service.ConsultaInventarioService;
import com.migal.smartops.recepciones.service.RecepcionService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Verifica que los parámetros de cada herramienta del agente coincidan con los de su operación en el contrato
 * (ADR-012, punto 7): el nombre existe en la operación, la obligatoriedad es la misma y los valores admitidos de un
 * catálogo cerrado no salen del enum del contrato.
 */
class HerramientasContratoTest {

	private static final Path MODULOS = Path.of("api", "modulos");
	private static final String BASE = "x-archivo";
	private static final List<String> SIN_PARAMETROS_DE_LISTADO = List.of("page", "pageSize", "orden");

	private final Map<Path, Map<String, Object>> documentos = new HashMap<>();

	@Test
	void losParametrosDeCadaHerramientaPertenecenASuOperacion() throws IOException {
		Map<String, Operacion> operaciones = operacionesDelContrato();
		for (DefinicionHerramienta herramienta : herramientas()) {
			Operacion operacion = operaciones.get(herramienta.nombre());
			assertNotNull(operacion, "Operación del contrato para " + herramienta.nombre());
			assertEquals(operacion.firma(), herramienta.operacion(), "Método y ruta de " + herramienta.nombre());
			for (ParametroHerramienta parametro : herramienta.parametros()) {
				Map<String, Object> delContrato = operacion.parametros().get(parametro.nombre());
				String contexto = herramienta.nombre() + "." + parametro.nombre();
				assertNotNull(delContrato, "Parámetro del contrato " + contexto);
				assertEquals(Boolean.TRUE.equals(delContrato.get("required")), parametro.obligatorio(),
						"Obligatoriedad de " + contexto);
				List<?> admitidos = enumDe(delContrato, (Path) delContrato.get(BASE));
				if (!parametro.valores().isEmpty() && admitidos != null) {
					assertTrue(admitidos.containsAll(parametro.valores()), "Valores de " + contexto);
				}
			}
			for (Map.Entry<String, Map<String, Object>> parametro : operacion.parametros().entrySet()) {
				if (Boolean.TRUE.equals(parametro.getValue().get("required"))) {
					assertTrue(herramienta.parametros().stream().anyMatch(p -> p.nombre().equals(parametro.getKey())),
							"La herramienta " + herramienta.nombre() + " declara el obligatorio " + parametro.getKey());
				}
			}
		}
	}

	@Test
	void ningunaHerramientaDeclaraParametrosDePaginacion() {
		for (DefinicionHerramienta herramienta : herramientas()) {
			assertFalse(herramienta.parametros().stream().anyMatch(p -> SIN_PARAMETROS_DE_LISTADO.contains(p.nombre())),
					"La paginación la fija el backend: " + herramienta.nombre());
		}
	}

	private static List<DefinicionHerramienta> herramientas() {
		return Stream.of(new HerramientasCatalogo(mock(ProductoService.class)),
				new HerramientasInventario(mock(ConsultaInventarioService.class)),
				new HerramientasDespacho(mock(DespachoService.class)),
				new HerramientasRecepciones(mock(RecepcionService.class)),
				new HerramientasVentas(mock(VentaService.class)))
				.flatMap(proveedor -> proveedor.definiciones().stream()).toList();
	}

	private record Operacion(String firma, Map<String, Map<String, Object>> parametros) {
	}

	@SuppressWarnings("unchecked")
	private Map<String, Operacion> operacionesDelContrato() throws IOException {
		Map<String, Operacion> operaciones = new HashMap<>();
		try (Stream<Path> archivos = Files.list(MODULOS)) {
			for (Path archivo : archivos.filter(a -> a.toString().endsWith(".yaml")).toList()) {
				Map<String, Object> rutas = (Map<String, Object>) cargar(archivo).get("paths");
				if (rutas == null) {
					continue;
				}
				for (Map.Entry<String, Object> ruta : rutas.entrySet()) {
					Map<String, Object> item = (Map<String, Object>) ruta.getValue();
					List<Object> comunes = (List<Object>) item.getOrDefault("parameters", List.of());
					for (Map.Entry<String, Object> metodo : item.entrySet()) {
						if (!(metodo.getValue() instanceof Map<?, ?> crudo) || crudo.get("operationId") == null) {
							continue;
						}
						Map<String, Object> op = (Map<String, Object>) crudo;
						List<Object> todos = new ArrayList<>(comunes);
						todos.addAll((List<Object>) op.getOrDefault("parameters", List.of()));
						Map<String, Map<String, Object>> parametros = new HashMap<>();
						for (Object parametro : todos) {
							Map.Entry<Map<String, Object>, Path> resuelto =
									resolverConBase((Map<String, Object>) parametro, archivo);
							Map<String, Object> conBase = new HashMap<>(resuelto.getKey());
							conBase.put(BASE, resuelto.getValue());
							parametros.put((String) conBase.get("name"), conBase);
						}
						operaciones.put((String) op.get("operationId"),
								new Operacion(metodo.getKey().toUpperCase() + " " + ruta.getKey(), parametros));
					}
				}
			}
		}
		return operaciones;
	}

	@SuppressWarnings("unchecked")
	private List<?> enumDe(Map<String, Object> parametro, Path base) throws IOException {
		Map<String, Object> esquema = resolver((Map<String, Object>) parametro.getOrDefault("schema", Map.of()), base);
		if (esquema.get("items") instanceof Map<?, ?> items) {
			esquema = resolver((Map<String, Object>) items, base);
		}
		return (List<?>) esquema.get("enum");
	}

	/** Sigue las referencias $ref locales y a otros archivos hasta el objeto definido. */
	@SuppressWarnings("unchecked")
	private Map<String, Object> resolver(Map<String, Object> nodo, Path base) throws IOException {
		return resolverConBase(nodo, base).getKey();
	}

	/** Objeto definido y archivo donde se encuentra, para resolver sus propias referencias relativas. */
	@SuppressWarnings("unchecked")
	private Map.Entry<Map<String, Object>, Path> resolverConBase(Map<String, Object> nodo, Path base)
			throws IOException {
		while (nodo.get("$ref") instanceof String referencia) {
			String[] partes = referencia.split("#", 2);
			Path archivo = partes[0].isEmpty() ? base : base.getParent().resolve(partes[0]).normalize();
			Object actual = cargar(archivo);
			for (String clave : partes[1].replaceFirst("^/", "").split("/")) {
				actual = ((Map<String, Object>) actual).get(clave);
			}
			nodo = (Map<String, Object>) actual;
			base = archivo;
		}
		return Map.entry(nodo, base);
	}

	private Map<String, Object> cargar(Path archivo) throws IOException {
		Path clave = archivo.toAbsolutePath().normalize();
		if (!documentos.containsKey(clave)) {
			try (InputStream entrada = Files.newInputStream(clave)) {
				documentos.put(clave, new Yaml().load(entrada));
			}
		}
		return documentos.get(clave);
	}
}
