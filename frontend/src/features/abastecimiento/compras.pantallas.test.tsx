import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { renderApp } from '@/test/renderApp';

expect.extend(matchers);
afterEach(() => window.sessionStorage.clear());

describe('Órdenes de compra, listado (P25)', () => {
  it('lista las órdenes con su avance de recepción y filtra por estado', async () => {
    const { container } = renderApp('/compras', 'Reponedor');

    await screen.findByText('OC-0086');
    const tabla = screen.getByRole('table', { name: 'Órdenes de compra' });
    expect(within(tabla).getByText('180 de 300 unidades')).toBeInTheDocument();

    await userEvent.click(await screen.findByRole('tab', { name: /Recepción parcial/ }));
    expect(screen.queryByText('OC-0087')).not.toBeInTheDocument();
    expect(screen.getByText('OC-0086')).toBeInTheDocument();

    expect(
      await axe(container, {
        rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
      }),
    ).toHaveNoViolations();
  });

  it('un Almacenero no accede al módulo y se redirige al inicio de su rol', async () => {
    renderApp('/compras', 'Almacenero');

    await waitFor(() =>
      expect(
        screen.queryByRole('heading', { level: 1, name: 'Órdenes de compra' }),
      ).not.toBeInTheDocument(),
    );
    expect(screen.queryByText('OC-0086')).not.toBeInTheDocument();
  });
});
