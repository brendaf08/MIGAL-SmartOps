import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { totales } from './calculos';
import { COTIZACIONES } from './fixtures';

afterEach(() => window.sessionStorage.clear());

describe('totales', () => {
  it('calcula subtotal, descuento y total', () => {
    const t = totales({
      descuento: 0.1,
      lineas: [{ id: 1, sku: 'A', nombre: 'A', cantidad: 10, precio: 5 }],
    });

    expect(t).toEqual({ subtotal: 50, descuento: 5, total: 45 });
  });
});

describe('Cotizaciones', () => {
  it('lista las cotizaciones y abre el detalle de una fila', async () => {
    renderApp('/cotizaciones', 'Vendedor');

    await userEvent.click(await screen.findByText('COT-0231'));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Cotización COT-0231' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Esta cotización no reserva existencias')).toBeInTheDocument();
  });

  it('presenta los totales con subtotal, descuento y total', async () => {
    renderApp('/cotizaciones/COT-0231', 'Vendedor');

    const table = await screen.findByRole('table', { name: 'Líneas de la cotización' });
    expect(within(table).getByText('Subtotal')).toBeInTheDocument();
    expect(within(table).getByText('Descuento (3 %)')).toBeInTheDocument();
    expect(within(table).getByText('Total')).toBeInTheDocument();
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
    expect(COTIZACIONES[0]!.estado).toBe('AceptadaCliente');
    COTIZACIONES[0]!.estado = 'Emitida';
  });

  it('avisa cuando la cotización no existe', async () => {
    renderApp('/cotizaciones/COT-9999', 'Vendedor');

    // La consulta fallida se reintenta una vez antes de informar el error.
    expect(
      await screen.findByText('La cotización no existe', undefined, { timeout: 4000 }),
    ).toBeInTheDocument();
  });
});
