import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { afterEach, beforeAll, describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { esquemaCotizacion, SIN_CLIENTE } from './cotizaciones/esquema';
import { COTIZACIONES } from './fixtures';

expect.extend(matchers);

/** Copia del estado inicial de las cotizaciones de prueba, para restaurarlo tras cada prueba. */
let inicial: typeof COTIZACIONES;
beforeAll(() => {
  inicial = JSON.parse(JSON.stringify(COTIZACIONES)) as typeof COTIZACIONES;
});

afterEach(() => {
  window.sessionStorage.clear();
  COTIZACIONES.splice(0, COTIZACIONES.length, ...JSON.parse(JSON.stringify(inicial)));
});

const CLIENTE_ANDES = /^Constructora Los Andes S\.A\.C\., 20456789012$/;
const PRODUCTO_CEMENTO = /^CEM-001,/;

/** Elige un cliente del selector; la opción se busca por su nombre completo. */
async function elegirCliente(opcion: RegExp | string) {
  await userEvent.click(screen.getByRole('combobox', { name: 'Cliente' }));
  await userEvent.click(await screen.findByRole('option', { name: opcion, hidden: true }));
}

/** Agrega una línea del formulario con producto, cantidad y precio, en la posición indicada. */
async function agregarLinea(producto: RegExp, cantidad: string, precio: string, n = 1) {
  await userEvent.click(screen.getByRole('button', { name: 'Agregar producto' }));
  await userEvent.click(screen.getByRole('combobox', { name: `Producto de la línea ${n}` }));
  await userEvent.click(await screen.findByRole('option', { name: producto, hidden: true }));
  const campoCantidad = screen.getByLabelText(`Cantidad de la línea ${n}`);
  await userEvent.clear(campoCantidad);
  await userEvent.type(campoCantidad, cantidad);
  const campoPrecio = screen.getByLabelText(`Precio de la línea ${n}`);
  await userEvent.clear(campoPrecio);
  await userEvent.type(campoPrecio, precio);
}

describe('Lista de cotizaciones', () => {
  it('filtra por estado y marca las cotizaciones que vencen en tres días o menos', async () => {
    renderApp('/cotizaciones', 'Vendedor');

    expect(await screen.findByText('COT-0231')).toBeInTheDocument();
    expect(screen.getByText(/vence en 3 días/)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('tab', { name: /Aceptadas/ }));
    expect(screen.getByText('COT-0230')).toBeInTheDocument();
    expect(screen.queryByText('COT-0231')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('tab', { name: /Vencidas/ }));
    expect(screen.getByText('COT-0229')).toBeInTheDocument();
    expect(screen.queryByText('COT-0230')).not.toBeInTheDocument();
  });

  it('busca por número o nombre de cliente', async () => {
    renderApp('/cotizaciones', 'Vendedor');
    await screen.findByText('COT-0231');

    await userEvent.type(screen.getByLabelText('Buscar por número o cliente'), 'Ferrocentro');

    expect(screen.getByText('COT-0230')).toBeInTheDocument();
    expect(screen.queryByText('COT-0231')).not.toBeInTheDocument();
  });

  it('abre el formulario de nueva cotización desde la acción de la página', async () => {
    renderApp('/cotizaciones', 'Vendedor');

    await userEvent.click(await screen.findByRole('button', { name: 'Nueva cotización' }));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Nueva cotización' }),
    ).toBeInTheDocument();
  });

  it('cumple la verificación de accesibilidad', async () => {
    const { container } = renderApp('/cotizaciones', 'Vendedor');
    await screen.findByText('COT-0231');

    expect(
      await axe(container, { rules: { 'color-contrast': { enabled: false } } }),
    ).toHaveNoViolations();
  });
});

describe('Emisión de cotizaciones', () => {
  it('emite una cotización con cliente y una línea, con el total correcto', async () => {
    renderApp('/cotizaciones/nueva', 'Vendedor');
    await screen.findByRole('heading', { level: 1, name: 'Nueva cotización' });

    await elegirCliente(CLIENTE_ANDES);
    await agregarLinea(PRODUCTO_CEMENTO, '2', '10');
    await userEvent.click(screen.getByRole('button', { name: 'Emitir cotización' }));

    const dialogo = await screen.findByRole('dialog');
    expect(within(dialogo).getByText(/Se emitirá con vencimiento el/)).toBeInTheDocument();
    expect(within(dialogo).getByText('La emisión no modifica el stock.')).toBeInTheDocument();
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Emitir cotización' }));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Cotización COT-0232' }),
    ).toBeInTheDocument();
    const tabla = await screen.findByRole('table', { name: 'Líneas de la cotización' });
    expect(within(tabla).getByText('Total').closest('tr')).toHaveTextContent('20.00');
    expect(COTIZACIONES[0]!.id).toBe('COT-0232');
  }, 15000);

  it('emite sin cliente', async () => {
    renderApp('/cotizaciones/nueva', 'Vendedor');
    await screen.findByRole('heading', { level: 1, name: 'Nueva cotización' });

    await elegirCliente('Continuar sin cliente');
    await agregarLinea(PRODUCTO_CEMENTO, '1', '5');
    await userEvent.click(screen.getByRole('button', { name: 'Emitir cotización' }));
    await userEvent.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Emitir cotización' }),
    );

    expect(await screen.findByText('Cotización sin cliente identificado.')).toBeInTheDocument();
  });

  it('no permite elegir un cliente inactivo y lo indica', async () => {
    renderApp('/cotizaciones/nueva', 'Vendedor');
    await screen.findByRole('heading', { level: 1, name: 'Nueva cotización' });

    await userEvent.click(screen.getByRole('combobox', { name: 'Cliente' }));
    const inactivo = await screen.findByRole('option', {
      name: /Constructora Misti/,
      hidden: true,
    });

    expect(inactivo).toHaveAttribute('aria-disabled', 'true');
    expect(screen.getByText('Los clientes inactivos no se pueden elegir.')).toBeInTheDocument();
  });

  it('deshabilita la emisión con un motivo visible cuando no hay líneas', async () => {
    renderApp('/cotizaciones/nueva', 'Vendedor');
    await screen.findByRole('heading', { level: 1, name: 'Nueva cotización' });

    expect(screen.getByRole('button', { name: 'Emitir cotización' })).toBeDisabled();
    expect(
      screen.getByText('Agregar al menos un producto para emitir la cotización.'),
    ).toBeInTheDocument();
  });

  it('valida el precio y la validez antes de emitir', async () => {
    renderApp('/cotizaciones/nueva', 'Vendedor');
    await screen.findByRole('heading', { level: 1, name: 'Nueva cotización' });

    await elegirCliente(CLIENTE_ANDES);
    await agregarLinea(PRODUCTO_CEMENTO, '2', '10');

    await userEvent.clear(screen.getByLabelText('Precio de la línea 1'));
    await userEvent.clear(screen.getByLabelText('Validez en días'));
    await userEvent.type(screen.getByLabelText('Validez en días'), '0');
    await userEvent.click(screen.getByRole('button', { name: 'Emitir cotización' }));

    expect(await screen.findByText('El precio debe ser mayor que 0.')).toBeInTheDocument();
    expect(screen.getByText('La validez debe estar entre 1 y 60 días.')).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('impide que el descuento supere el subtotal', async () => {
    renderApp('/cotizaciones/nueva', 'Vendedor');
    await screen.findByRole('heading', { level: 1, name: 'Nueva cotización' });

    await agregarLinea(PRODUCTO_CEMENTO, '2', '10');
    const descuento = screen.getByLabelText('Descuento en soles');
    await userEvent.clear(descuento);
    await userEvent.type(descuento, '999');
    await userEvent.tab();

    expect(descuento).toHaveValue('S/ 20.00');
  });

  it('rechaza en el esquema un descuento mayor que el subtotal y una cantidad no entera', () => {
    const base = {
      clienteId: 'CLI-001',
      lineas: [{ id: 1, sku: 'CEM-001', cantidad: 2, precio: 10 }],
      descuento: 25,
      validezDias: 7,
      formaPago: 'Contado',
      observaciones: '',
    };

    const conDescuento = esquemaCotizacion.safeParse(base);
    expect(conDescuento.success).toBe(false);
    expect(conDescuento.error?.issues[0]?.path).toEqual(['descuento']);

    const conCantidad = esquemaCotizacion.safeParse({
      ...base,
      descuento: 0,
      lineas: [{ id: 1, sku: 'CEM-001', cantidad: 1.5, precio: 10 }],
    });
    expect(conCantidad.success).toBe(false);
  });

  it('acepta el valor de sin cliente en el esquema', () => {
    const resultado = esquemaCotizacion.safeParse({
      clienteId: SIN_CLIENTE,
      lineas: [{ id: 1, sku: 'CEM-001', cantidad: 1, precio: 10 }],
      descuento: 0,
      validezDias: 1,
      formaPago: 'Contado',
      observaciones: '',
    });
    expect(resultado.success).toBe(true);
  });

  it('cumple la verificación de accesibilidad', async () => {
    const { container } = renderApp('/cotizaciones/nueva', 'Vendedor');
    await screen.findByRole('heading', { level: 1, name: 'Nueva cotización' });

    expect(
      await axe(container, { rules: { 'color-contrast': { enabled: false } } }),
    ).toHaveNoViolations();
  });
});

describe('Modificación de cotizaciones', () => {
  it('modifica una cotización emitida', async () => {
    renderApp('/cotizaciones/COT-0231/editar', 'Vendedor');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Editar cotización' }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText('Cantidad de la línea 1')).toHaveValue('200');

    await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('Cotización modificada')).toBeInTheDocument();
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Cotización COT-0231' }),
    ).toBeInTheDocument();
  });

  it('muestra una cotización aceptada como solo lectura', async () => {
    renderApp('/cotizaciones/COT-0230/editar', 'Vendedor');

    expect(await screen.findByText('Cotización de solo lectura')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Guardar cambios' })).toBeDisabled();
    expect(
      screen.getByText('La cotización no se puede modificar en su estado actual.'),
    ).toBeInTheDocument();
  });

  it('no está disponible para un rol sin acceso al módulo', async () => {
    renderApp('/cotizaciones/nueva', 'Almacenero');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Disponibilidad' }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('heading', { level: 1, name: 'Nueva cotización' }),
    ).not.toBeInTheDocument();
  });
});

describe('Aceptación y rechazo desde el detalle', () => {
  it('registra el rechazo tras confirmar y cambia el estado', async () => {
    renderApp('/cotizaciones/COT-0231', 'Vendedor');

    await userEvent.click(await screen.findByRole('button', { name: 'Registrar rechazo' }));
    const dialogo = await screen.findByRole('dialog');
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Registrar rechazo' }));

    expect(await screen.findByText('Rechazo registrado')).toBeInTheDocument();
    await waitFor(() => expect(COTIZACIONES[0]!.estado).toBe('Rechazada'));
  });

  it('registra la aceptación y cambia el estado', async () => {
    renderApp('/cotizaciones/COT-0231', 'Vendedor');

    await userEvent.click(await screen.findByRole('button', { name: 'Registrar aceptación' }));
    const dialogoAceptar = await screen.findByRole('dialog', {
      name: /Registrar la aceptación de la cotización/,
    });
    await userEvent.click(
      within(dialogoAceptar).getByRole('button', { name: 'Registrar aceptación' }),
    );

    expect(await screen.findByText('Aceptación registrada')).toBeInTheDocument();
    await waitFor(() => expect(COTIZACIONES[0]!.estado).toBe('AceptadaCliente'));
  });

  it('ofrece convertir en venta una cotización aceptada', async () => {
    renderApp('/cotizaciones/COT-0230', 'Vendedor');

    const enlace = await screen.findByRole('link', { name: 'Convertir en venta' });
    expect(enlace).toHaveAttribute('href', '/ventas/nueva?cotizacion=COT-0230');
  });
});
