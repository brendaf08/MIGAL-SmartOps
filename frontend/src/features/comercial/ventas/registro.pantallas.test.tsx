import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { renderApp } from '@/test/renderApp';
import {
  calcularAtencion,
  tieneLineasSinCobertura,
  type LineaSolicitada,
  type ModalidadAtencion,
} from './registro/atencion';

expect.extend(matchers);
afterEach(() => window.sessionStorage.clear());

const stock = { CEM: 40, FIE: 10, TUB: 0 };
const linea = (sku: string, cantidad: number): LineaSolicitada => ({ sku, cantidad });

describe('calcularAtencion', () => {
  it('atiende todas las líneas y deja la venta pendiente de despacho si hay stock', () => {
    const r = calcularAtencion([linea('CEM', 40), linea('FIE', 10)], stock, null);

    expect(r.lineas.map((l) => [l.atendida, l.pendiente])).toEqual([
      [40, 0],
      [10, 0],
    ]);
    expect(r).toMatchObject({
      totalPendiente: 0,
      estado: 'PendienteDespacho',
      entrega: 'Completa',
    });
  });

  it('con modalidad parcial atiende lo disponible y registra la diferencia como pendiente', () => {
    const r = calcularAtencion([linea('CEM', 55)], stock, 'parcial');

    expect(r.lineas[0]).toEqual({ sku: 'CEM', solicitada: 55, atendida: 40, pendiente: 15 });
    expect(r).toMatchObject({
      totalPendiente: 15,
      estado: 'PendienteAbastecimiento',
      entrega: 'Parcial',
    });
  });

  it.each<ModalidadAtencion>(['pendienteAbastecimiento', 'pendienteCliente'])(
    'con %s no atiende las líneas sin cobertura, pero sí las cubiertas',
    (modalidad) => {
      const r = calcularAtencion([linea('CEM', 40), linea('FIE', 11)], stock, modalidad);

      expect(r.lineas.map((l) => l.atendida)).toEqual([40, 0]);
      expect(r.lineas[1]!.pendiente).toBe(11);
      expect(r).toMatchObject({ estado: 'PendienteAbastecimiento', entrega: 'Parcial' });
    },
  );

  it('sin modalidad no atiende lo no cubierto', () => {
    expect(calcularAtencion([linea('TUB', 3)], stock, null).lineas[0]!.atendida).toBe(0);
  });

  it('una cantidad exactamente igual al stock queda cubierta', () => {
    expect(tieneLineasSinCobertura([linea('CEM', 40)], stock)).toBe(false);
    expect(tieneLineasSinCobertura([linea('CEM', 41)], stock)).toBe(true);
  });

  it('con stock cero o negativo no atiende nada en modo parcial', () => {
    const r = calcularAtencion([linea('TUB', 5), linea('SIN', 2)], { TUB: 0, SIN: -4 }, 'parcial');

    expect(r.lineas.map((l) => l.atendida)).toEqual([0, 0]);
  });

  it('varias líneas del mismo SKU comparten el stock y nunca lo superan', () => {
    const r = calcularAtencion([linea('CEM', 25), linea('CEM', 25)], stock, 'parcial');

    expect(r.lineas.map((l) => l.atendida)).toEqual([25, 15]);
    expect(r.lineas.reduce((s, l) => s + l.atendida, 0)).toBe(40);
  });

  it('ninguna combinación atiende más que el stock ni más que lo solicitado', () => {
    const existencias = [0, 1, 5, 10];
    const cantidades = [1, 2, 5, 9, 10, 11];
    const modalidades: (ModalidadAtencion | null)[] = [
      null,
      'parcial',
      'pendienteAbastecimiento',
      'pendienteCliente',
    ];
    for (const libre of existencias)
      for (const cantidad of cantidades)
        for (const m of modalidades)
          for (const cantidad2 of cantidades) {
            const r = calcularAtencion(
              [linea('X', cantidad), linea('X', cantidad2)],
              { X: libre },
              m,
            );
            const atendido = r.lineas.reduce((s, l) => s + l.atendida, 0);
            expect(atendido).toBeLessThanOrEqual(libre);
            r.lineas.forEach((l) => {
              expect(l.atendida).toBeLessThanOrEqual(l.solicitada);
              expect(l.atendida + l.pendiente).toBe(l.solicitada);
            });
          }
  });
});

/*
 * Datos de prueba de inventario: CEM-001 tiene stock libre suficiente y FIE-003 no tiene existencias.
 * Ver src/features/inventario/fixtures.ts.
 */
const TIEMPO = 15000;

/** Agrega una línea con producto y cantidad en la posición indicada (1, 2...). */
async function agregarLinea(n: number, sku: string, cantidad: number) {
  await userEvent.click(screen.getByRole('button', { name: 'Agregar producto' }));
  await userEvent.click(screen.getByRole('combobox', { name: `Producto de la línea ${n}` }));
  const opciones = await screen.findAllByRole('option', { hidden: true });
  await userEvent.click(opciones.find((o) => o.textContent?.startsWith(`${sku},`))!);
  const campo = screen.getByRole('textbox', { name: `Cantidad de la línea ${n}` });
  await userEvent.clear(campo);
  await userEvent.type(campo, String(cantidad));
}

async function elegirCliente(texto: string) {
  await userEvent.click(screen.getByRole('combobox', { name: 'Cliente' }));
  await userEvent.click(await screen.findByRole('option', { name: texto, hidden: true }));
}

describe('Registro de venta', () => {
  it(
    'registra una venta con stock suficiente y muestra la constancia',
    async () => {
      renderApp('/ventas/nueva', 'Vendedor');
      await elegirCliente('Constructora Los Andes S.A.C. (20456789012)');
      await agregarLinea(1, 'CEM-001', 10);

      await userEvent.click(screen.getByRole('button', { name: 'Confirmar venta' }));
      const dialogo = await screen.findByRole('dialog', { name: 'Confirmar la venta' });
      expect(
        within(dialogo).getByText('Se registrarán 1 salidas en el Kardex.'),
      ).toBeInTheDocument();
      await userEvent.click(within(dialogo).getByRole('button', { name: 'Confirmar venta' }));

      expect(
        await screen.findByRole('status', { name: /Venta V-\d{4} registrada/ }, { timeout: 4000 }),
      ).toBeInTheDocument();
      expect(screen.getByRole('link', { name: 'Ver la venta' })).toBeInTheDocument();
    },
    TIEMPO,
  );

  it(
    'exige elegir modalidad si el stock no alcanza y registra con atención parcial',
    async () => {
      renderApp('/ventas/nueva', 'Vendedor');
      await elegirCliente('Constructora Los Andes S.A.C. (20456789012)');
      await agregarLinea(1, 'FIE-003', 5);

      expect(
        await screen.findByText('Elegir cómo atender las líneas sin existencias.'),
      ).toBeInTheDocument();
      await userEvent.click(screen.getByRole('radio', { name: 'Atender parcialmente' }));
      await userEvent.click(screen.getByRole('button', { name: 'Confirmar venta' }));
      const dialogo = await screen.findByRole('dialog', { name: 'Confirmar la venta' });
      expect(within(dialogo).getByText('Quedan 5 unidades pendientes.')).toBeInTheDocument();
      await userEvent.click(within(dialogo).getByRole('button', { name: 'Confirmar venta' }));

      expect(
        await screen.findByText('Quedan 5 unidades pendientes de abastecimiento.', undefined, {
          timeout: 4000,
        }),
      ).toBeInTheDocument();
    },
    TIEMPO,
  );

  it(
    'rechaza un descuento mayor que el subtotal',
    async () => {
      renderApp('/ventas/nueva', 'Vendedor');
      await elegirCliente('Constructora Los Andes S.A.C. (20456789012)');
      await agregarLinea(1, 'CEM-001', 1);

      const descuento = screen.getByRole('textbox', { name: 'Descuento en soles' });
      await userEvent.clear(descuento);
      await userEvent.type(descuento, '999999');

      expect(
        await screen.findByText('El descuento no puede ser negativo ni mayor que el subtotal.'),
      ).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Confirmar venta' })).toBeDisabled();
    },
    TIEMPO,
  );

  it(
    'admite una venta sin cliente identificado',
    async () => {
      renderApp('/ventas/nueva', 'Vendedor');
      await elegirCliente('Continuar sin cliente');
      await agregarLinea(1, 'CEM-001', 2);

      await userEvent.click(screen.getByRole('button', { name: 'Confirmar venta' }));
      const dialogo = await screen.findByRole('dialog', { name: 'Confirmar la venta' });
      await userEvent.click(within(dialogo).getByRole('button', { name: 'Confirmar venta' }));

      expect(
        await screen.findByRole('status', { name: /Venta V-\d{4} registrada/ }, { timeout: 4000 }),
      ).toBeInTheDocument();
    },
    TIEMPO,
  );

  it('bloquea la confirmación sin líneas y muestra el motivo', async () => {
    renderApp('/ventas/nueva', 'Vendedor');
    await elegirCliente('Continuar sin cliente');

    expect(screen.getByRole('button', { name: 'Confirmar venta' })).toBeDisabled();
    expect(await screen.findByText('Agregar al menos un producto.')).toBeInTheDocument();
  });

  it('carga las líneas de una cotización aceptada', async () => {
    renderApp('/ventas/nueva?cotizacion=COT-0230', 'Vendedor');

    expect(await screen.findByRole('combobox', { name: 'Producto de la línea 1' })).toHaveValue(
      'TUB-006, Tubo PVC desagüe 4 in x 3 m',
    );
  });

  it('no ofrece una cotización que no está aceptada', async () => {
    renderApp('/ventas/nueva?cotizacion=COT-0231', 'Vendedor');

    expect(
      await screen.findByText('La cotización no está aceptada o ya se convirtió en venta.'),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('combobox', { name: 'Producto de la línea 1' }),
    ).not.toBeInTheDocument();
  });

  it('restringe la ruta a un rol autorizado', async () => {
    renderApp('/ventas/nueva', 'Almacenero');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Disponibilidad' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Registrar venta' })).not.toBeInTheDocument();
  });

  it('el formulario no tiene violaciones de accesibilidad', async () => {
    const { container } = renderApp('/ventas/nueva', 'Vendedor');

    await screen.findByRole('heading', { level: 1, name: 'Registrar venta' });
    await waitFor(() => expect(container.querySelector('form')).not.toBeNull());
    expect(
      await axe(container, { rules: { 'color-contrast': { enabled: false } } }),
    ).toHaveNoViolations();
  });
});
