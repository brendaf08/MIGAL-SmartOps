import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { renderApp } from '@/test/renderApp';

expect.extend(matchers);
afterEach(() => window.sessionStorage.clear());

describe('Proveedores: lista', () => {
  it('muestra los proveedores al Reponedor y no ofrece la pestaña de órdenes de compra', async () => {
    renderApp('/proveedores', 'Reponedor');

    expect(await screen.findByRole('heading', { level: 1, name: 'Proveedores' })).toBeInTheDocument();
    expect(await screen.findByText('Distribuidora Andina S.A.C.')).toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: /Órdenes de compra/ })).not.toBeInTheDocument();
  });

  it('impide el acceso a un rol que no es Reponedor', async () => {
    renderApp('/proveedores', 'Vendedor');

    await waitFor(() =>
      expect(screen.getByRole('heading', { level: 1 })).not.toHaveTextContent('Proveedores'),
    );
    expect(screen.queryByRole('button', { name: 'Registrar proveedor' })).not.toBeInTheDocument();
  });

  it('filtra por estado con las pestañas', async () => {
    const user = userEvent.setup();
    renderApp('/proveedores', 'Reponedor');
    await screen.findByText('Distribuidora Andina S.A.C.');

    await user.click(screen.getByRole('tab', { name: 'Inactivos' }));

    const tabla = () => screen.getByRole('table', { name: 'Proveedores' });
    await waitFor(() =>
      expect(within(tabla()).getByText('Cementos Arequipa E.I.R.L.')).toBeInTheDocument(),
    );
    expect(within(tabla()).queryByText('Distribuidora Andina S.A.C.')).not.toBeInTheDocument();
  });

  it('busca por razón social o RUC', async () => {
    const user = userEvent.setup();
    renderApp('/proveedores', 'Reponedor');
    await screen.findByText('Distribuidora Andina S.A.C.');

    await user.type(screen.getByRole('searchbox', { name: 'Buscar proveedor' }), '20234567890');

    expect(await screen.findByText('Aceros del Sur S.A.', {}, { timeout: 4000 })).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.queryByText('Distribuidora Andina S.A.C.')).not.toBeInTheDocument(),
    );
  });

  it('no tiene violaciones de accesibilidad', async () => {
    const { container } = renderApp('/proveedores', 'Reponedor');
    await screen.findByText('Distribuidora Andina S.A.C.');

    expect(
      await axe(container, { rules: { 'color-contrast': { enabled: false } } }),
    ).toHaveNoViolations();
  });
});
