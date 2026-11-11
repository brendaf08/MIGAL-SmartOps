import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { notifications } from '@mantine/notifications';
import { renderApp } from '@/test/renderApp';
import {
  cancelarVenta,
  completarVenta,
  existenciaDe,
  planAtencion,
  reiniciarAjustes,
  totalesVenta,
} from './api';
import { VENTAS } from './fixtures';

expect.extend(matchers);

/** Copia de los datos de prueba: cada prueba los modifica y al terminar se restablecen. */
const original = JSON.parse(JSON.stringify(VENTAS)) as typeof VENTAS;
const restaurar = () => {
  VENTAS.splice(0, VENTAS.length, ...JSON.parse(JSON.stringify(original)));
  reiniciarAjustes();
};

beforeEach(restaurar);
afterEach(() => {
  restaurar();
  window.sessionStorage.clear();
});

const venta = (id: string) => VENTAS.find((v) => v.id === id)!;

describe('Lista de ventas', () => {
  it('cuenta por estado, filtra y marca las ventas con pendientes', async () => {
    renderApp('/ventas', 'Vendedor');

    const tabla = await screen.findByRole('table', { name: 'Ventas' });
    expect(await within(tabla).findByText('V-0040')).toBeInTheDocument();
    expect(within(tabla).getByText('Pendiente: 810 unidades')).toBeInTheDocument();

    const pestana = screen.getByRole('tab', { name: /Pendiente de abastecimiento/ });
    expect(pestana).toHaveTextContent('2');
    await userEvent.click(pestana);

    expect(within(tabla).getByText('V-0042')).toBeInTheDocument();
    expect(within(tabla).queryByText('V-0036')).not.toBeInTheDocument();
    expect(within(tabla).getByText('Pendiente: 100 unidades')).toBeInTheDocument();
  });

  it('busca por número de venta o por cliente', async () => {
    renderApp('/ventas', 'Vendedor');
    const tabla = await screen.findByRole('table', { name: 'Ventas' });

    await userEvent.type(screen.getByRole('searchbox'), 'Rivera');

    await waitFor(() => expect(within(tabla).queryByText('V-0040')).not.toBeInTheDocument());
    expect(within(tabla).getByText('V-0041')).toBeInTheDocument();
  });

  it('abre el detalle al hacer clic en una fila', async () => {
    renderApp('/ventas', 'Vendedor');

    await userEvent.click(await screen.findByText('V-0036'));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Venta V-0036' }),
    ).toBeInTheDocument();
  });

  it('no permite a un Almacenero acceder al módulo', async () => {
    renderApp('/ventas', 'Almacenero');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Disponibilidad' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('table', { name: 'Ventas' })).not.toBeInTheDocument();
  });
});

describe('Detalle de la venta', () => {
  it('muestra solicitada, atendida y pendiente, con los totales', async () => {
    renderApp('/ventas/V-0036', 'Vendedor');

    await screen.findByRole('heading', { level: 1, name: 'Venta V-0036' });
    const fila = screen.getByRole('row', { name: /Tubo PVC/ });
    expect(within(fila).getByText('80')).toBeInTheDocument();
    expect(within(fila).getByText('50')).toBeInTheDocument();
    expect(within(fila).getByText('30')).toBeInTheDocument();
    // Subtotal 80 x 16.90 = 1,352.00; sin descuento, el total coincide.
    expect(screen.getAllByText(/1,352\.00/).length).toBeGreaterThanOrEqual(2);
    expect(screen.getByText('Hay cantidades pendientes de atención')).toBeInTheDocument();
  });

  it('informa la atención parcial y la disponibilidad de cada producto pendiente', async () => {
    renderApp('/ventas/V-0040', 'Vendedor');

    expect(
      await screen.findByText(/Atención parcial: se atendieron 1,200 de 2,010/),
    ).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /Disponibilidad de Ladrillo/ })).toBeDefined();
  });

  it('completa una venta con stock suficiente, la deja lista para despacho y muestra la constancia', async () => {
    // Se deja pendiente solo lo que cubre el stock libre de CAB-007 (3 unidades).
    venta('V-0040').lineas[0]!.atendida = 2000;
    venta('V-0040').lineas[1]!.solicitada = 2;
    renderApp('/ventas/V-0040', 'Vendedor');

    await userEvent.click(await screen.findByRole('button', { name: 'Completar venta' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Completar la venta V-0040' });
    expect(within(dialogo).getByRole('button', { name: 'Volver' })).toHaveFocus();
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Completar venta' }));

    const constancia = await screen.findByRole('status', {
      name: 'Venta V-0040 lista para despacho',
    });
    expect(constancia).toHaveTextContent('Se atendieron 2 unidades con el stock libre.');
    expect(venta('V-0040').estado).toBe('ListaDespacho');
  });

  it('completa con stock parcial: atiende lo posible y deja el resto pendiente', async () => {
    const libre = existenciaDe('CAB-007').libre;
    renderApp('/ventas/V-0040', 'Vendedor');

    await userEvent.click(await screen.findByRole('button', { name: 'Completar venta' }));
    await userEvent.click(
      within(await screen.findByRole('dialog', { name: 'Completar la venta V-0040' })).getByRole(
        'button',
        { name: 'Completar venta' },
      ),
    );

    const constancia = await screen.findByRole('status', {
      name: 'Venta V-0040 atendida en parte',
    });
    expect(constancia).toHaveTextContent(`Se atendieron ${libre} unidades con el stock libre.`);
    expect(constancia).toHaveTextContent(`Quedan pendientes ${800 + (10 - libre)} unidades`);
    expect(venta('V-0040').estado).toBe('PendienteAbastecimiento');
    expect(venta('V-0040').lineas[0]!.atendida).toBe(1200);
    expect(venta('V-0040').lineas[1]!.atendida).toBe(libre);
  });

  it('sin stock, deshabilita la acción y muestra el motivo', async () => {
    renderApp('/ventas/V-0042', 'Vendedor');

    expect(await screen.findByRole('button', { name: 'Completar venta' })).toBeDisabled();
    expect(
      screen.getByText('Sin stock disponible para las unidades pendientes.'),
    ).toBeInTheDocument();
  });

  it('nunca atiende más que el stock libre ni más que lo pendiente', () => {
    const plan = planAtencion(venta('V-0040'));
    const cable = plan.find((a) => a.sku === 'CAB-007')!;
    const ladrillo = plan.find((a) => a.sku === 'LAD-002')!;

    expect(cable.atendible).toBe(Math.min(cable.pendiente, cable.libre));
    expect(ladrillo.atendible).toBe(0);
  });

  it('cancelar exige confirmación y deja la venta cancelada', async () => {
    renderApp('/ventas/V-0040', 'Vendedor');

    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar venta' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Cancelar la venta V-0040' });
    expect(dialogo).toHaveTextContent('La acción no se puede deshacer.');
    expect(within(dialogo).getByRole('button', { name: 'Volver' })).toHaveFocus();
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Cancelar venta' }));

    await waitFor(() =>
      expect(screen.queryByRole('button', { name: 'Cancelar venta' })).not.toBeInTheDocument(),
    );
    expect(venta('V-0040').estado).toBe('Cancelada');
  });

  it('no ofrece cancelar una venta despachada', async () => {
    renderApp('/ventas/V-0035', 'Vendedor');

    await screen.findByRole('heading', { level: 1, name: 'Venta V-0035' });
    expect(screen.queryByRole('button', { name: 'Cancelar venta' })).not.toBeInTheDocument();
  });

  it('muestra un aviso con enlace de regreso si la venta no existe', async () => {
    renderApp('/ventas/V-9999', 'Vendedor');

    expect(await screen.findByText('La venta no existe')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Volver al listado de ventas' })).toHaveAttribute(
      'href',
      '/ventas',
    );
  });
});

describe('Lógica de atención', () => {
  it('calcula subtotal, descuento y total de la venta', () => {
    expect(totalesVenta(venta('V-0040'))).toEqual({ subtotal: 4400, descuento: 20, total: 4380 });
  });

  it('la completación reserva solo lo atendible y actualiza el stock libre', async () => {
    const antes = existenciaDe('CAB-007').libre;
    await completarVenta('V-0040', 'clave-completar-prueba');

    expect(existenciaDe('CAB-007').libre).toBe(antes - venta('V-0040').lineas[1]!.atendida);
    expect(existenciaDe('CAB-007').libre).toBe(0);
  });

  it('no permite cancelar una venta ya despachada', async () => {
    await expect(cancelarVenta('V-0035', 'Motivo de prueba', 'clave-cancelar-prueba')).rejects.toThrow();
  });
});

describe('Accesibilidad (axe)', () => {
  it('lista, detalle y diálogos no tienen violaciones', async () => {
    notifications.clean();
    const { unmount } = renderApp('/ventas', 'Vendedor');
    await within(await screen.findByRole('table', { name: 'Ventas' })).findByText('V-0040');
    expect(
      await axe(document.body, { rules: { 'color-contrast': { enabled: false } } }),
    ).toHaveNoViolations();
    unmount();

    renderApp('/ventas/V-0040', 'Vendedor');
    await userEvent.click(await screen.findByRole('button', { name: 'Completar venta' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Completar la venta V-0040' });
    // El encabezado del diálogo de Mantine es un <header> dentro del portal: axe lo cuenta como segundo banner.
    expect(
      await axe(dialogo, {
        rules: {
          'color-contrast': { enabled: false },
          'landmark-no-duplicate-banner': { enabled: false },
        },
      }),
    ).toHaveNoViolations();
  });
});
