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

/** Elige una opción del selector de rol. */
async function elegirRol(rol: string) {
  await userEvent.click(screen.getByRole('combobox', { name: 'Rol' }));
  // El desplegable de Mantine se anima en jsdom y sus opciones quedan marcadas como ocultas durante la transición.
  await userEvent.click(await screen.findByRole('option', { name: rol, hidden: true }));
}

describe('Usuarios: registro y edición de rol', () => {
  it('un Almacenero no accede al formulario de registro', async () => {
    renderApp('/usuarios/nuevo', 'Almacenero');

    await waitFor(() =>
      expect(screen.queryByRole('heading', { name: 'Registrar usuario', level: 1 })).toBeNull(),
    );
  }, 20000);

  it('registrar un usuario válido lo agrega a la lista', async () => {
    renderApp('/usuarios/nuevo', 'Administrador');

    await userEvent.type(screen.getByRole('textbox', { name: 'Nombre completo' }), 'Ana Ríos');
    await userEvent.type(screen.getByRole('textbox', { name: 'Usuario' }), 'arios');
    await userEvent.type(screen.getByLabelText(/Contraseña temporal/), 'clave1234');
    await elegirRol('Vendedor');
    await userEvent.click(screen.getByRole('button', { name: 'Registrar usuario' }));

    const t = await tablaCon('arios');
    expect(within(t).getByText('Ana Ríos')).toBeInTheDocument();
  }, 20000);

  it('un usuario duplicado muestra el error junto al campo y conserva los datos', async () => {
    renderApp('/usuarios/nuevo', 'Administrador');

    await userEvent.type(screen.getByRole('textbox', { name: 'Nombre completo' }), 'Otro Admin');
    await userEvent.type(screen.getByRole('textbox', { name: 'Usuario' }), 'admin');
    await userEvent.type(screen.getByLabelText(/Contraseña temporal/), 'clave1234');
    await elegirRol('Administrador');
    await userEvent.click(screen.getByRole('button', { name: 'Registrar usuario' }));

    expect(
      await screen.findByText('El usuario ya está registrado. Ingresar uno distinto.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Nombre completo' })).toHaveValue('Otro Admin');
    expect(screen.getByRole('textbox', { name: 'Usuario' })).toHaveValue('admin');
  }, 20000);

  it('cada regla de validación muestra su mensaje', async () => {
    renderApp('/usuarios/nuevo', 'Administrador');

    await userEvent.click(screen.getByRole('button', { name: 'Registrar usuario' }));
    expect(
      await screen.findByText('Ingresar el nombre completo, con al menos 3 caracteres.'),
    ).toBeInTheDocument();
    expect(screen.getByText('Ingresar un usuario de 4 a 20 caracteres.')).toBeInTheDocument();
    expect(
      screen.getByText('La contraseña temporal debe tener al menos 8 caracteres.'),
    ).toBeInTheDocument();
    expect(screen.getByText('Seleccionar un rol.')).toBeInTheDocument();

    await userEvent.type(screen.getByRole('textbox', { name: 'Usuario' }), 'Ana R');
    await userEvent.type(screen.getByLabelText(/Contraseña temporal/), 'sololetras');
    await userEvent.click(screen.getByRole('button', { name: 'Registrar usuario' }));
    expect(
      await screen.findByText('Usar solo minúsculas, números y los signos . _ -, sin espacios.'),
    ).toBeInTheDocument();
    expect(screen.getByText('La contraseña temporal debe incluir un número.')).toBeInTheDocument();
  }, 20000);

  it('modificar el rol actualiza la fila', async () => {
    renderApp('/usuarios/U-002', 'Administrador');

    await screen.findByDisplayValue('Lucía Torres');
    await elegirRol('Almacenero');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    const t = await tablaCon('esoto');
    const fila = within(t).getByText('Lucía Torres').closest('tr')!;
    expect(within(fila).getByText('Almacenero')).toBeInTheDocument();
  }, 20000);
});
