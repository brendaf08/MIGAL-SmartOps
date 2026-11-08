import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { PRODUCTOS } from '@/features/inventario/fixtures';

afterEach(() => window.sessionStorage.clear());

const contar = (predicado: (sku: string, categoria: string) => boolean) =>
  PRODUCTOS.filter((p) => predicado(p.sku, p.categoria)).length;

describe('Catálogo, lista', () => {
  it('muestra 20 productos por página y cambia de página', async () => {
    renderApp('/catalogo', 'Almacenero');
    const table = await screen.findByRole('table', { name: 'Catálogo de productos' });
    await within(table).findByText(PRODUCTOS[0]!.sku);
    expect(within(table).getAllByRole('row')).toHaveLength(21);

    await userEvent.click(screen.getByRole('button', { name: 'Página siguiente' }));

    await within(table).findByText(PRODUCTOS[20]!.sku);
  });

  it('busca por SKU y filtra por categoría', async () => {
    renderApp('/catalogo', 'Almacenero');
    const table = await screen.findByRole('table', { name: 'Catálogo de productos' });
    await within(table).findByText(PRODUCTOS[0]!.sku);

    await userEvent.type(screen.getByRole('searchbox', { name: 'Buscar producto' }), 'CEM-0');
    const cemento = contar((sku) => sku.startsWith('CEM-'));
    await waitFor(() => expect(within(table).getAllByRole('row')).toHaveLength(cemento + 1));

    await userEvent.clear(screen.getByRole('searchbox', { name: 'Buscar producto' }));
    await userEvent.click(screen.getByRole('combobox', { name: 'Filtrar por categoría' }));
    await userEvent.click(await screen.findByRole('option', { name: 'Ladrillos', hidden: true }));
    const ladrillos = contar((_, c) => c === 'Ladrillos');
    await waitFor(() => expect(within(table).getAllByRole('row')).toHaveLength(ladrillos + 1));
  });

  it('filtra por estado y muestra el estado vacío sin coincidencias', async () => {
    renderApp('/catalogo', 'Almacenero');
    await screen.findByRole('table', { name: 'Catálogo de productos' });

    await userEvent.click(screen.getByRole('combobox', { name: 'Filtrar por estado' }));
    await userEvent.click(await screen.findByRole('option', { name: 'Inactivo', hidden: true }));

    expect(await screen.findByText('Sin resultados')).toBeInTheDocument();
  });

  it('no permite a un Vendedor acceder al catálogo', async () => {
    renderApp('/catalogo', 'Vendedor');

    expect(
      await screen.findByRole('table', { name: 'Disponibilidad por producto' }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('table', { name: 'Catálogo de productos' }),
    ).not.toBeInTheDocument();
  });
});
