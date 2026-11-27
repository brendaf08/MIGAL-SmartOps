import { cleanup, fireEvent, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderApp } from '@/test/renderApp';
import type * as RegistroVentaApi from './registro/api';

vi.mock('./registro/api', async (importOriginal) => {
  const actual = await importOriginal<typeof RegistroVentaApi>();
  return {
    ...actual,
    // Simula la lista asíncrona de la API; la resolución local inicial de cotizacionUtilizable no aporta datos.
    cotizacionUtilizable: () => undefined,
    listarCotizacionesAceptadas: async () => [
      {
        id: '812',
        estado: 'ValidadaMigal',
        emision: '2026-11-02',
        vencimiento: '2026-11-20',
        cliente: { id: '57', nombre: 'Constructora Andina S.A.C.' },
        descuento: 0,
        lineas: [
          {
            id: 1,
            sku: 'CEM-001',
            nombre: 'Cemento Sol tipo I, bolsa 42.5 kg',
            cantidad: 2,
            precio: 29.9,
          },
        ],
      },
    ],
  };
});

afterEach(() => {
  cleanup();
  window.sessionStorage.clear();
});

describe('conversión de cotización en venta', () => {
  it('precarga desde la consulta asíncrona cuando se abre con el ID de la cotización', async () => {
    renderApp('/ventas/nueva?cotizacion=812', 'Vendedor');

    expect(await screen.findByRole('combobox', { name: 'Producto de la línea 1' })).toHaveValue(
      'CEM-001, Cemento Sol tipo I, bolsa 42.5 kg',
    );
    // La fecha de entrega requerida es obligatoria: sin ella no se puede confirmar.
    expect(screen.getByRole('button', { name: 'Confirmar venta' })).toBeDisabled();
    fireEvent.change(screen.getByLabelText(/Fecha de entrega requerida/), {
      target: { value: '2099-12-31' },
    });
    expect(screen.getByRole('button', { name: 'Confirmar venta' })).toBeEnabled();
  });
});
