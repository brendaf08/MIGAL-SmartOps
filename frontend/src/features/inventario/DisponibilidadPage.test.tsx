import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { resumen } from './calculos';
import { PRODUCTOS } from './fixtures';

afterEach(() => window.sessionStorage.clear());

const enAlerta = PRODUCTOS.filter((p) => resumen(p).nivel !== 'Normal').length;

describe('DisponibilidadPage', () => {
  it('lista la primera página de productos con su nivel de stock', async () => {
    renderApp('/disponibilidad', 'Almacenero');

    await screen.findByText(PRODUCTOS[0]!.sku);
    const table = screen.getByRole('table', { name: 'Disponibilidad por producto' });
    expect(within(table).getAllByRole('row')).toHaveLength(21);
    expect(screen.getByRole('tab', { name: /Todos/ })).toHaveTextContent(String(PRODUCTOS.length));
  });

  it('filtra los productos en alerta con la pestaña y su contador', async () => {
    renderApp('/disponibilidad', 'Almacenero');
    const tab = await screen.findByRole('tab', { name: /En alerta/ });
    await within(tab).findByText(String(enAlerta));

    await userEvent.click(tab);

    await screen.findAllByText(/En el mínimo|Stock crítico/);
    const table = screen.getByRole('table', { name: 'Disponibilidad por producto' });
    await waitFor(() =>
      expect(within(table).queryByText('Stock suficiente')).not.toBeInTheDocument(),
    );
  });

  it('muestra el estado vacío con la opción de limpiar filtros', async () => {
    renderApp('/disponibilidad', 'Vendedor');

    await userEvent.type(await screen.findByRole('searchbox', { name: 'Buscar producto' }), 'zzz');

    await userEvent.click(await screen.findByRole('button', { name: 'Limpiar filtros' }));
    expect(await screen.findByRole('searchbox', { name: 'Buscar producto' })).toHaveValue('');
  });

  it('abre el detalle del producto con sus existencias por ubicación', async () => {
    renderApp('/disponibilidad', 'Almacenero');
    const first = PRODUCTOS[0]!;

    await userEvent.click(await screen.findByText(first.sku));

    const dialog = await screen.findByRole('dialog', { name: first.nombre });
    expect(within(dialog).getByText('Tienda principal')).toBeInTheDocument();
    expect(within(dialog).getByText('Almacén 2')).toBeInTheDocument();
  });
});
