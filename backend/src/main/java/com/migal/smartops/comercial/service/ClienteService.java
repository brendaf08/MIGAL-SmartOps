package com.migal.smartops.comercial.service;

import com.migal.smartops.comercial.model.Cliente;
import com.migal.smartops.comercial.model.TipoDocumentoCliente;
import com.migal.smartops.comercial.repository.ClienteRepository;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administración de clientes del módulo comercial. El documento de identificación es opcional en las operaciones de
 * mostrador; si se informa, el tipo y el número se informan juntos y el número respeta su formato. Un documento no
 * puede pertenecer a dos clientes. La desactivación es lógica y conserva el historial.
 */
@Service
public class ClienteService {

	private static final Pattern DNI = Pattern.compile("^[0-9]{8}$");
	private static final Pattern RUC = Pattern.compile("^(10|20)[0-9]{9}$");

	/**
	 * Datos registrables de un cliente. Los textos se normalizan al construir el registro: los vacíos se convierten en
	 * nulos y los demás se recortan.
	 */
	public record DatosCliente(TipoDocumentoCliente tipoDocumento, String numeroDocumento, String nombreRazonSocial,
			String telefonoContacto, String direccionReferencia) {

		public DatosCliente {
			numeroDocumento = limpiar(numeroDocumento);
			nombreRazonSocial = limpiar(nombreRazonSocial);
			telefonoContacto = limpiar(telefonoContacto);
			direccionReferencia = limpiar(direccionReferencia);
		}
	}

	private final ClienteRepository clientes;

	public ClienteService(ClienteRepository clientes) {
		this.clientes = clientes;
	}

	/**
	 * Lista paginada de clientes. El filtro {@code q} busca por nombre o razón social y por número de documento, sin
	 * distinguir mayúsculas. Si la vigencia se omite, se devuelven clientes activos e inactivos.
	 */
	@Transactional(readOnly = true)
	public Page<Cliente> listar(String q, Boolean estadoActivo, Pageable paginacion) {
		Specification<Cliente> filtros = (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			if (q != null && !q.isBlank()) {
				String patron = "%" + q.trim().toLowerCase() + "%";
				condiciones.add(cb.or(
						cb.like(cb.lower(raiz.get("nombreRazonSocial")), patron),
						cb.like(cb.lower(raiz.get("numeroDocumento")), patron)));
			}
			if (estadoActivo != null) {
				condiciones.add(cb.equal(raiz.get("estadoActivo"), estadoActivo));
			}
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
		return clientes.findAll(filtros, paginacion);
	}

	/** Consulta un cliente, activo o inactivo. */
	@Transactional(readOnly = true)
	public Cliente consultar(long id) {
		return clientes.findById(id).orElseThrow(ClienteService::noEncontrado);
	}

	/**
	 * Registra un cliente activo. Si el documento ya pertenece a otro cliente responde {@code DOCUMENTO_CLIENTE_DUPLICADO}.
	 */
	@Transactional
	public Cliente registrar(DatosCliente datos) {
		validar(datos);
		verificarDocumentoLibre(datos, null);
		return guardar(new Cliente(datos.tipoDocumento(), datos.numeroDocumento(), datos.nombreRazonSocial(),
				datos.telefonoContacto(), datos.direccionReferencia()));
	}

	/** Modifica los datos del cliente. La vigencia se cambia mediante {@link #cambiarEstado(long, boolean)}. */
	@Transactional
	public Cliente actualizar(long id, DatosCliente datos) {
		Cliente cliente = consultar(id);
		validar(datos);
		verificarDocumentoLibre(datos, id);
		cliente.actualizar(datos.tipoDocumento(), datos.numeroDocumento(), datos.nombreRazonSocial(),
				datos.telefonoContacto(), datos.direccionReferencia());
		return guardar(cliente);
	}

	/** Activa o desactiva el cliente. La desactivación es lógica y conserva su historial. */
	@Transactional
	public Cliente cambiarEstado(long id, boolean activo) {
		Cliente cliente = consultar(id);
		cliente.cambiarEstado(activo);
		return clientes.save(cliente);
	}

	/**
	 * Verifica que el cliente exista y esté activo. Lo usan las cotizaciones antes de asociarse a un cliente.
	 *
	 * @throws ErrorDeNegocio {@code VALIDACION_FALLIDA} si no existe y {@code CLIENTE_INACTIVO} si está inactivo
	 */
	@Transactional(readOnly = true)
	public Cliente validarVigente(long id) {
		Cliente cliente = clientes.findById(id)
				.orElseThrow(() -> validacion("El cliente indicado no existe."));
		if (!cliente.isEstadoActivo()) {
			throw new ErrorDeNegocio(CodigoError.CLIENTE_INACTIVO, "El cliente está inactivo.");
		}
		return cliente;
	}

	private Cliente guardar(Cliente cliente) {
		try {
			return clientes.saveAndFlush(cliente);
		} catch (DataIntegrityViolationException e) {
			throw documentoDuplicado(null);
		}
	}

	private static void validar(DatosCliente datos) {
		if (datos.nombreRazonSocial() == null || datos.nombreRazonSocial().length() > 200) {
			throw validacion("El nombre o razón social es obligatorio y no puede superar 200 caracteres.");
		}
		boolean tieneTipo = datos.tipoDocumento() != null;
		boolean tieneNumero = datos.numeroDocumento() != null;
		if (tieneTipo != tieneNumero) {
			throw validacion("El tipo y el número de documento se informan juntos o se omiten juntos.");
		}
		if (datos.tipoDocumento() == TipoDocumentoCliente.DNI && !DNI.matcher(datos.numeroDocumento()).matches()) {
			throw validacion("El DNI debe tener 8 dígitos.");
		}
		if (datos.tipoDocumento() == TipoDocumentoCliente.RUC && !RUC.matcher(datos.numeroDocumento()).matches()) {
			throw validacion("El RUC debe tener 11 dígitos y empezar en 10 o 20.");
		}
		if (datos.telefonoContacto() != null && datos.telefonoContacto().length() > 30) {
			throw validacion("El teléfono de contacto no puede superar 30 caracteres.");
		}
		if (datos.direccionReferencia() != null && datos.direccionReferencia().length() > 255) {
			throw validacion("La dirección o referencia no puede superar 255 caracteres.");
		}
	}

	/** Un documento informado no puede pertenecer a otro cliente. Un identificador nulo indica un cliente nuevo. */
	private void verificarDocumentoLibre(DatosCliente datos, Long idActual) {
		if (datos.tipoDocumento() == null) {
			return;
		}
		clientes.findByTipoDocumentoAndNumeroDocumento(datos.tipoDocumento().valor(), datos.numeroDocumento())
				.filter(existente -> !existente.getId().equals(idActual))
				.ifPresent(existente -> {
					throw documentoDuplicado(existente.getId());
				});
	}

	private static String limpiar(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}

	private static ErrorDeNegocio validacion(String detalle) {
		return new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, detalle);
	}

	/** Error de documento duplicado. Incluye el cliente titular cuando se conoce, como exige el contrato. */
	private static ErrorDeNegocio documentoDuplicado(Long idExistente) {
		Map<String, Object> propiedades = idExistente == null ? Map.of() : Map.of("clienteId", idExistente.toString());
		return new ErrorDeNegocio(CodigoError.DOCUMENTO_CLIENTE_DUPLICADO, "El documento ya pertenece a otro cliente.",
				propiedades);
	}

	private static ErrorDeNegocio noEncontrado() {
		return new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El cliente no existe.");
	}
}
