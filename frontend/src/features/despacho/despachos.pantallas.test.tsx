import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { ORDENES } from './fixtures';
import { REIMPRESIONES } from './ordenes/api';

expect.extend(matchers);

/** Quita las órdenes creadas por cada prueba para que no afecten a las siguientes. */
afterEach(() => {
  window.sessionStorage.clear();
  const i = ORDENES.findIndex((o) => o.id === 'D-0032');
  if (i >= 0) ORDENES.splice(i, 1);
  REIMPRESIONES.length = 0;
  vi.restoreAllMocks();
});

describe('Lista de despachos', () => {
  it('agrupa las órdenes por pestaña con su recuento', async () => {
    renderApp('/despachos', 'Almacenero');

    const pendientes = await screen.findByRole('tab', { name: /Pendientes de despacho/ });
    await waitFor(() => expect(within(pendientes).getByText('2')).toBeInTheDocument());
    expect(await screen.findByText('D-0030')).toBeInTheDocument();
    expect(screen.queryByText('D-0029')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('tab', { name: /Entregadas/ }));

    expect(await screen.findByText('D-0029')).toBeInTheDocument();
    expect(screen.queryByText('D-0030')).not.toBeInTheDocument();
  });

  it('avisa con texto cuando la reserva de una orden no cubre la cantidad', async () => {
    renderApp('/despachos', 'Almacenero');

    expect(await screen.findByText('Reserva incompleta')).toBeInTheDocument();
  });

  it('genera una orden Emitida desde una venta lista tras la confirmación', async () => {
    renderApp('/despachos', 'Almacenero');

    await userEvent.click(
      await screen.findByRole('button', { name: 'Generar orden de despacho para V-0041' }),
    );
    const dialog = await screen.findByRole('dialog', {
      name: 'Generar la orden de despacho de la venta V-0041',
    });
    expect(dialog).toHaveTextContent('Dirección de entrega: Av. Argentina 1870, Callao.');
    expect(dialog).toHaveTextContent('Cemento Sol tipo I, bolsa 42.5 kg');

    await userEvent.click(
      within(dialog).getByRole('button', { name: 'Generar orden de despacho' }),
    );

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Orden de despacho D-0032' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Emitida')).toBeInTheDocument();
    expect(ORDENES.find((o) => o.id === 'D-0032')?.estado).toBe('Emitida');
  });

  it('exige el motivo para reimprimir la orden de una venta que ya la tiene', async () => {
    renderApp('/despachos', 'Almacenero');

    await userEvent.click(
      await screen.findByRole('button', { name: 'Generar orden de despacho para V-0038' }),
    );
    const dialog = await screen.findByRole('dialog', { name: 'La venta ya tiene la orden D-0031' });
    expect(dialog).toHaveTextContent('Reimprimir exige una validación adicional');

    await userEvent.click(within(dialog).getByRole('button', { name: 'Reimprimir orden' }));

    expect(await within(dialog).findByText('Indicar el motivo de la reimpresión.')).toBeVisible();
    expect(REIMPRESIONES).toHaveLength(0);

    await userEvent.type(
      within(dialog).getByRole('textbox', { name: 'Motivo de la reimpresión' }),
      'Impresión dañada',
    );
    await userEvent.click(within(dialog).getByRole('button', { name: 'Reimprimir orden' }));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Orden de despacho D-0031' }),
    ).toBeInTheDocument();
    expect(REIMPRESIONES).toEqual([{ ordenId: 'D-0031', motivo: 'Impresión dañada' }]);
  });

  it('no permite a un Vendedor acceder a los despachos', async () => {
    renderApp('/despachos', 'Vendedor');

    const titulo = await screen.findByRole('heading', { level: 1 });
    expect(titulo).not.toHaveTextContent('Despachos');
  });

  it('no tiene violaciones de accesibilidad', async () => {
    const { container } = renderApp('/despachos', 'Almacenero');

    await screen.findByText('D-0030');

    expect(
      await axe(container, { rules: { 'color-contrast': { enabled: false } } }),
    ).toHaveNoViolations();
  });
});

describe('Orden de despacho imprimible', () => {
  it('muestra los datos de la venta y los tres espacios de firma', async () => {
    renderApp('/despachos/D-0031/imprimir', 'Almacenero');

    const documento = await screen.findByRole('article');
    expect(
      within(documento).getByRole('heading', { level: 1, name: 'Orden de despacho D-0031' }),
    ).toBeInTheDocument();
    expect(documento).toHaveTextContent('V-0038');
    expect(documento).toHaveTextContent('Constructora Los Andes S.A.C.');
    expect(documento).toHaveTextContent('Av. Javier Prado Este 4200, San Borja, Lima');
    for (const firma of ['Despachó', 'Chofer', 'Recibió']) {
      expect(within(documento).getByText(firma)).toBeInTheDocument();
    }
  });

  it('invoca window.print desde el botón Imprimir', async () => {
    const print = vi.spyOn(window, 'print').mockImplementation(() => {});
    renderApp('/despachos/D-0031/imprimir', 'Almacenero');

    await userEvent.click(await screen.findByRole('button', { name: 'Imprimir' }));

    expect(print).toHaveBeenCalledTimes(1);
  });

  it('no tiene violaciones de accesibilidad', async () => {
    const { container } = renderApp('/despachos/D-0031/imprimir', 'Almacenero');

    await screen.findByRole('article');

    expect(
      await axe(container, { rules: { 'color-contrast': { enabled: false } } }),
    ).toHaveNoViolations();
  });
});

describe('Ruta de despachos imprimibles', () => {
  it('se conserva la ficha de la orden en su ruta', async () => {
    renderApp('/despachos/D-0031', 'Almacenero');

    await waitFor(() =>
      expect(
        screen.getByRole('heading', { level: 1, name: 'Orden de despacho D-0031' }),
      ).toBeInTheDocument(),
    );
  });
});
