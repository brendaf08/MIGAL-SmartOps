import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { afterEach, describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';

expect.extend(matchers);
afterEach(() => window.sessionStorage.clear());

/** Espera a que la tabla cargue (aparece la fila indicada) y la devuelve. */
async function tablaCon(texto: string) {
  const t = await screen.findByRole('table', { name: 'Usuarios' });
  await within(t).findByText(texto);
  return t;
}

describe('Usuarios: lista', () => {
  it('el Administrador ve la tabla de usuarios con su rol y estado', async () => {
    renderApp('/usuarios', 'Administrador');

    const t = await tablaCon('rquispe');
    expect(within(t).getAllByRole('cell', { name: 'Almacenero' }).length).toBeGreaterThan(0);
    expect(within(t).getAllByText('Inactivo').length).toBeGreaterThan(0);
  });

  it('un Almacenero no accede a /usuarios y se redirige al inicio', async () => {
    renderApp('/usuarios', 'Almacenero');

    await waitFor(() => expect(screen.queryByRole('table', { name: 'Usuarios' })).toBeNull());
    expect(screen.queryByRole('heading', { name: 'Usuarios y roles' })).toBeNull();
  });

  it('filtra por estado con las pestañas', async () => {
    renderApp('/usuarios', 'Administrador');
    await tablaCon('rquispe');

    await userEvent.click(screen.getByRole('tab', { name: /Inactivos/ }));
    const t = await tablaCon('phuaman');
    expect(within(t).queryByText('admin')).toBeNull();
  });

  it('busca por nombre o usuario', async () => {
    renderApp('/usuarios', 'Administrador');
    await tablaCon('rquispe');

    await userEvent.type(screen.getByRole('searchbox', { name: 'Buscar usuario' }), 'esoto');
    await waitFor(() =>
      expect(within(screen.getByRole('table', { name: 'Usuarios' })).queryByText('admin')).toBeNull(),
    );
    expect(within(screen.getByRole('table', { name: 'Usuarios' })).getByText('esoto')).toBeInTheDocument();
  });

  it('no tiene violaciones de accesibilidad', async () => {
    const { container } = renderApp('/usuarios', 'Administrador');
    await tablaCon('rquispe');

    expect(
      await axe(container, { rules: { 'color-contrast': { enabled: false } } }),
    ).toHaveNoViolations();
  });
});
