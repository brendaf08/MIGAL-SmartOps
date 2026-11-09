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

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Proveedores' }),
    ).toBeInTheDocument();
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

    expect(
      await screen.findByText('Aceros del Sur S.A.', {}, { timeout: 4000 }),
    ).toBeInTheDocument();
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

const llenarDatosValidos = async (
  user: ReturnType<typeof userEvent.setup>,
  ruc = '20999888777',
) => {
  await user.type(
    screen.getByRole('textbox', { name: /Razón social/ }),
    'Importadora Nueva S.A.C.',
  );
  await user.type(screen.getByRole('textbox', { name: /RUC/ }), ruc);
  await user.type(screen.getByRole('textbox', { name: /Persona de contacto/ }), 'Mario Ríos');
  await user.type(
    screen.getByRole('textbox', { name: /Condiciones comerciales/ }),
    'Pago a 30 días',
  );
};

describe('Proveedores: formulario', () => {
  it('registra un proveedor válido con productos asociados y vuelve a la lista', async () => {
    const user = userEvent.setup();
    renderApp('/proveedores/nueva', 'Reponedor');
    await screen.findByRole('heading', { level: 1, name: 'Registrar proveedor' });

    await llenarDatosValidos(user);
    const selector = screen.getByRole('combobox', { name: /Productos del catálogo/ });
    await user.click(selector);
    await user.type(selector, 'CEM-001');
    await user.click(await screen.findByText(/^CEM-001,/));
    await user.click(screen.getByRole('button', { name: 'Registrar proveedor' }));

    expect(
      await screen.findByText('Importadora Nueva S.A.C.', {}, { timeout: 4000 }),
    ).toBeInTheDocument();
  }, 20000);

  it('muestra el error de RUC duplicado con el enlace al proveedor existente', async () => {
    const user = userEvent.setup();
    renderApp('/proveedores/nueva', 'Reponedor');
    await screen.findByRole('heading', { level: 1, name: 'Registrar proveedor' });

    await llenarDatosValidos(user, '20123456789');
    await user.click(screen.getByRole('button', { name: 'Registrar proveedor' }));

    expect(await screen.findByText('Este RUC ya está registrado.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Modificar el proveedor existente' })).toHaveAttribute(
      'href',
      '/proveedores/P-001',
    );
  }, 20000);

  it('valida el RUC, el teléfono, el correo y los campos obligatorios', async () => {
    const user = userEvent.setup();
    renderApp('/proveedores/nueva', 'Reponedor');
    await screen.findByRole('heading', { level: 1, name: 'Registrar proveedor' });

    await user.click(screen.getByRole('button', { name: 'Registrar proveedor' }));
    expect(await screen.findByText('Ingresar la razón social.')).toBeInTheDocument();
    await user.type(screen.getByRole('textbox', { name: /RUC/ }), '123');
    await user.type(screen.getByRole('textbox', { name: /Teléfono/ }), '12345');
    await user.type(screen.getByRole('textbox', { name: /Correo electrónico/ }), 'abc');
    await user.click(screen.getByRole('button', { name: 'Registrar proveedor' }));
    expect(await screen.findByText('El RUC debe tener 11 dígitos.')).toBeInTheDocument();
    expect(screen.getByText('El teléfono debe tener 9 dígitos.')).toBeInTheDocument();
    expect(screen.getByText('Ingresar un correo válido.')).toBeInTheDocument();

    await user.clear(screen.getByRole('textbox', { name: /RUC/ }));
    await user.type(screen.getByRole('textbox', { name: /RUC/ }), '30123456789');
    await user.click(screen.getByRole('button', { name: 'Registrar proveedor' }));
    expect(await screen.findByText('El RUC debe empezar con 10 o 20.')).toBeInTheDocument();
  }, 20000);

  it('avisa cuando el SKU buscado no está en el catálogo y permite continuar sin asociar', async () => {
    const user = userEvent.setup();
    renderApp('/proveedores/nueva', 'Reponedor');
    await screen.findByRole('heading', { level: 1, name: 'Registrar proveedor' });

    await user.type(screen.getByRole('combobox', { name: /Productos del catálogo/ }), 'XYZ-999');

    expect(await screen.findByText('El producto no está en el catálogo')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Dar de alta en el catálogo' })).toHaveAttribute(
      'href',
      '/catalogo/nuevo',
    );

    await user.click(screen.getByRole('button', { name: 'Continuar sin asociar' }));
    await waitFor(() =>
      expect(screen.queryByText('El producto no está en el catálogo')).not.toBeInTheDocument(),
    );
  });

  it('muestra un proveedor inactivo de solo lectura y permite activarlo con confirmación', async () => {
    const user = userEvent.setup();
    renderApp('/proveedores/P-003', 'Reponedor');

    expect(
      await screen.findByText(
        'Este proveedor está inactivo y no puede usarse en nuevas órdenes de compra',
      ),
    ).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: /Razón social/ })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Guardar cambios' })).toBeDisabled();
    expect(screen.getByText('Activar el proveedor para modificarlo.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Activar proveedor' }));
    const dialogo = await screen.findByRole('dialog', {
      name: 'Activar a Cementos Arequipa E.I.R.L.',
    });
    await user.click(within(dialogo).getByRole('button', { name: 'Activar proveedor' }));

    await waitFor(() =>
      expect(
        screen.queryByText(
          'Este proveedor está inactivo y no puede usarse en nuevas órdenes de compra',
        ),
      ).not.toBeInTheDocument(),
    );
  });

  it('pide confirmación antes de desactivar un proveedor', async () => {
    const user = userEvent.setup();
    renderApp('/proveedores/P-002', 'Reponedor');
    await screen.findByRole('heading', { level: 1, name: 'Aceros del Sur S.A.' });

    await user.click(screen.getByRole('button', { name: 'Desactivar proveedor' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Desactivar a Aceros del Sur S.A.' });
    await user.click(within(dialogo).getByRole('button', { name: 'Cancelar' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(screen.getByRole('button', { name: 'Desactivar proveedor' })).toBeInTheDocument();
  });

  it('impide el acceso a un rol que no es Reponedor', async () => {
    renderApp('/proveedores/nueva', 'Vendedor');

    await waitFor(() =>
      expect(
        screen.queryByRole('heading', { name: 'Registrar proveedor' }),
      ).not.toBeInTheDocument(),
    );
  });

  it('no tiene violaciones de accesibilidad en el formulario', async () => {
    const { container } = renderApp('/proveedores/nueva', 'Reponedor');
    await screen.findByRole('heading', { level: 1, name: 'Registrar proveedor' });

    expect(
      await axe(container, { rules: { 'color-contrast': { enabled: false } } }),
    ).toHaveNoViolations();
  });
});
