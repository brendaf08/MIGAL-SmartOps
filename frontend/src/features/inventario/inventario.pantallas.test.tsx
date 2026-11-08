import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { renderApp } from '@/test/renderApp';
import { resumen } from './calculos';
import { MOVIMIENTOS, PRODUCTOS } from './fixtures';

expect.extend(matchers);

/** Los registros de movimiento modifican los datos de prueba en memoria: cada caso parte del estado inicial. */
const copia = <T,>(valor: T): T => JSON.parse(JSON.stringify(valor)) as T;
const ESTADO_INICIAL = { productos: copia(PRODUCTOS), movimientos: copia(MOVIMIENTOS) };

afterEach(() => {
  window.sessionStorage.clear();
  PRODUCTOS.splice(0, PRODUCTOS.length, ...copia(ESTADO_INICIAL.productos));
  MOVIMIENTOS.splice(0, MOVIMIENTOS.length, ...copia(ESTADO_INICIAL.movimientos));
});

/** Espera a que la tabla reciba datos: antes de la carga muestra filas de esqueleto. */
const esperarFilas = (tabla: HTMLElement) =>
  within(tabla).findAllByText(/Stock crítico|En el mínimo/);

const contar = (nivel: 'Critico' | 'Minimo') =>
  PRODUCTOS.filter((p) => resumen(p).nivel === nivel).length;

describe('Alertas de stock (P12)', () => {
  it('ordena las alertas por gravedad y muestra los contadores', async () => {
    renderApp('/alertas', 'Almacenero');

    const tabla = await screen.findByRole('table', { name: 'Productos en alerta' });
    await esperarFilas(tabla);
    const criticos = contar('Critico');
    const minimos = contar('Minimo');
    const niveles = within(tabla)
      .getAllByRole('row')
      .slice(1)
      .map((fila) => (within(fila).queryByText('Stock crítico') ? 'Critico' : 'Minimo'));
    expect(niveles).toEqual([
      ...Array<string>(criticos).fill('Critico'),
      ...Array<string>(minimos).fill('Minimo'),
    ]);

    const etiqueta = (texto: string) => screen.getByText(texto).parentElement!;
    expect(within(etiqueta('Críticos')).getByText(String(criticos))).toBeInTheDocument();
    expect(
      within(etiqueta('Total en alerta')).getByText(String(criticos + minimos)),
    ).toBeInTheDocument();
  });

  it('filtra por nivel con las pestañas', async () => {
    renderApp('/alertas', 'Almacenero');
    await userEvent.click(await screen.findByRole('tab', { name: /Críticas/ }));

    const tabla = screen.getByRole('table', { name: 'Productos en alerta' });
    await esperarFilas(tabla);
    expect(within(tabla).queryByText('En el mínimo')).not.toBeInTheDocument();
    expect(within(tabla).getAllByText('Stock crítico')).toHaveLength(contar('Critico'));
  });

  it('muestra la acción de crear orden de compra solo al Reponedor', async () => {
    renderApp('/alertas', 'Reponedor');
    expect(
      (await screen.findAllByRole('button', { name: 'Crear orden de compra' })).length,
    ).toBeGreaterThan(0);
  });

  it('no muestra la acción de crear orden de compra al Almacenero', async () => {
    renderApp('/alertas', 'Almacenero');
    const tabla = await screen.findByRole('table', { name: 'Productos en alerta' });
    await esperarFilas(tabla);
    expect(
      screen.queryByRole('button', { name: 'Crear orden de compra' }),
    ).not.toBeInTheDocument();
  });

  it('no permite al Vendedor acceder a las alertas', async () => {
    renderApp('/alertas', 'Vendedor');
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Disponibilidad' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Alertas de stock' })).not.toBeInTheDocument();
  });

  it('no tiene violaciones de accesibilidad (axe)', async () => {
    const { container } = renderApp('/alertas', 'Almacenero');
    await screen.findByRole('table', { name: 'Productos en alerta' });
    expect(
      await axe(container, { rules: { 'color-contrast': { enabled: false } } }),
    ).toHaveNoViolations();
  });
});
