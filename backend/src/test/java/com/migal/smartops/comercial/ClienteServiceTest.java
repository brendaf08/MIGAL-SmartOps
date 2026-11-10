package com.migal.smartops.comercial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.comercial.model.TipoDocumentoCliente;
import com.migal.smartops.comercial.service.ClienteService;
import com.migal.smartops.comercial.service.ClienteService.DatosCliente;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/** Verifica las reglas del servicio de clientes con PostgreSQL real. Cada prueba se revierte al terminar. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ClienteServiceTest {

	@Autowired
	private ClienteService clientes;

	/** Número aleatorio de la longitud indicada, con el prefijo dado. */
	private static String numero(String prefijo, int longitud) {
		StringBuilder sb = new StringBuilder(prefijo);
		while (sb.length() < longitud) {
			sb.append(ThreadLocalRandom.current().nextInt(10));
		}
		return sb.toString();
	}

	private static DatosCliente ruc(String numero) {
		return new DatosCliente(TipoDocumentoCliente.RUC, numero, "Constructora de prueba", null, null);
	}

	private static DatosCliente dni(String numero) {
		return new DatosCliente(TipoDocumentoCliente.DNI, numero, "Persona de prueba", "987654321", null);
	}

	private static void assertCodigo(CodigoError codigo, Executable accion) {
		assertEquals(codigo, assertThrows(ErrorDeNegocio.class, accion).codigo());
	}

	@Test
	void registraUnClienteActivoConDocumentoValido() {
		var cliente = clientes.registrar(ruc(numero("20", 11)));
		assertTrue(cliente.isEstadoActivo());
		assertEquals(TipoDocumentoCliente.RUC, cliente.getTipoDocumento());
	}

	@Test
	void aceptaVariosClientesDeMostradorSinDocumento() {
		clientes.registrar(new DatosCliente(null, null, "Mostrador uno", null, null));
		var segundo = clientes.registrar(new DatosCliente(null, null, "Mostrador dos", null, null));
		assertNull(segundo.getTipoDocumento());
	}

	@Test
	void rechazaUnDocumentoQueYaPerteneceAOtroCliente() {
		String documento = numero("1", 8);
		clientes.registrar(dni(documento));
		assertCodigo(CodigoError.DOCUMENTO_CLIENTE_DUPLICADO, () -> clientes.registrar(dni(documento)));
	}

	@Test
	void permiteActualizarUnClienteConSuPropioDocumento() {
		String documento = numero("20", 11);
		var cliente = clientes.registrar(ruc(documento));
		assertEquals(documento, clientes.actualizar(cliente.getId(), ruc(documento)).getNumeroDocumento());
	}

	@Test
	void validaElFormatoDelDniYDelRuc() {
		assertCodigo(CodigoError.VALIDACION_FALLIDA, () -> clientes.registrar(dni("1234567")));
		assertCodigo(CodigoError.VALIDACION_FALLIDA, () -> clientes.registrar(ruc("15601234567")));
		assertCodigo(CodigoError.VALIDACION_FALLIDA,
				() -> clientes.registrar(new DatosCliente(TipoDocumentoCliente.RUC, null, "Sin numero", null, null)));
	}

	@Test
	void desactivaSinEliminarYFiltraPorVigencia() {
		var cliente = clientes.registrar(ruc(numero("20", 11)));
		assertFalse(clientes.cambiarEstado(cliente.getId(), false).isEstadoActivo());
		assertEquals(cliente.getId(), clientes.consultar(cliente.getId()).getId());
		var activos = clientes.listar(null, true, PageRequest.of(0, 500));
		assertTrue(activos.getContent().stream().noneMatch(c -> c.getId().equals(cliente.getId())));
	}

	@Test
	void validaQueElClienteExistaYEsteActivo() {
		var cliente = clientes.registrar(dni(numero("1", 8)));
		clientes.cambiarEstado(cliente.getId(), false);
		assertCodigo(CodigoError.CLIENTE_INACTIVO, () -> clientes.validarVigente(cliente.getId()));
		assertCodigo(CodigoError.VALIDACION_FALLIDA, () -> clientes.validarVigente(-1L));
	}
}
