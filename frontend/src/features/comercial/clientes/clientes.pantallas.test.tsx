import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { AppProviders } from '@/app/AppProviders';
import { renderApp } from '@/test/renderApp';
import { ClienteFormModal } from './ClienteFormModal';
import { ClientePicker, SIN_CLIENTE } from './ClientePicker';

expect.extend(matchers);
afterEach(() => window.sessionStorage.clear());

/** Selector de cliente con la salida del valor elegido, como en una venta. */
function Selector() {
  const [valor, setValor] = useState<string | null>(null);
  return (
    <AppProviders>
      <ClientePicker value={valor} onChange={setValor} />
      <output data-testid="elegido">{valor ?? 'ninguno'}</output>
    </AppProviders>
  );
}

/** Elige una opción de un selector de Mantine por su nombre accesible. */
async function elegirOpcion(selector: string, opcion: string) {
  await userEvent.click(screen.getByRole('combobox', { name: selector }));
  await userEvent.click(await screen.findByRole('option', { name: opcion, hidden: true }));
}

/** Completa el formulario de cliente con los datos indicados. */
async function llenarFormulario(datos: {
  tipo?: 'DNI' | 'RUC';
  documento: string;
  nombre: string;
  telefono?: string;
  correo?: string;
}) {
  if (datos.tipo) await elegirOpcion('Tipo de documento', datos.tipo);
  await userEvent.type(screen.getByRole('textbox', { name: /^Documento/ }), datos.documento);
  await userEvent.type(
    screen.getByRole('textbox', { name: /^Nombre o razón social/ }),
    datos.nombre,
  );
  if (datos.telefono)
    await userEvent.type(screen.getByRole('textbox', { name: /^Teléfono/ }), datos.telefono);
  if (datos.correo)
    await userEvent.type(screen.getByRole('textbox', { name: /^Correo/ }), datos.correo);
}

const sinContraste = { rules: { 'color-contrast': { enabled: false } } };

/** Las pruebas que completan formularios escriben varios campos; con la carga de la suite completa superan el límite por omisión de 5 s. */
const TIEMPO_FORMULARIO = 15000;

describe('ClientePicker', () => {
  it(
    'registra un cliente desde el selector y lo deja seleccionado',
    async () => {
      render(<Selector />);

      await userEvent.click(screen.getByRole('button', { name: 'Registrar cliente nuevo' }));
      const dialogo = await screen.findByRole('dialog', { name: 'Registrar cliente' });
      await llenarFormulario({ documento: '71234567', nombre: 'Ferretería Nueva Sol' });
      await userEvent.click(within(dialogo).getByRole('button', { name: 'Registrar cliente' }));

      await waitFor(() =>
        expect(screen.getByRole('combobox', { name: 'Cliente' })).toHaveValue(
          'Ferretería Nueva Sol (71234567)',
        ),
      );
      expect(screen.getByTestId('elegido')).not.toHaveTextContent('ninguno');
    },
    TIEMPO_FORMULARIO,
  );

  it(
    'valida el número de documento según el tipo',
    async () => {
      render(<Selector />);

      await userEvent.click(screen.getByRole('button', { name: 'Registrar cliente nuevo' }));
      await llenarFormulario({ documento: '1234', nombre: 'Prueba de validación' });
      await userEvent.click(screen.getByRole('button', { name: 'Registrar cliente' }));
      expect(await screen.findByText('El DNI tiene 8 dígitos.')).toBeInTheDocument();

      await elegirOpcion('Tipo de documento', 'RUC');
      await userEvent.clear(screen.getByRole('textbox', { name: /^Documento/ }));
      await userEvent.type(screen.getByRole('textbox', { name: /^Documento/ }), '30123456789');
      await userEvent.click(screen.getByRole('button', { name: 'Registrar cliente' }));
      expect(
        await screen.findByText('El RUC tiene 11 dígitos y empieza en 10 o 20.'),
      ).toBeInTheDocument();
    },
    TIEMPO_FORMULARIO,
  );

  it(
    'valida el teléfono y el correo cuando se escriben',
    async () => {
      render(<Selector />);

      await userEvent.click(screen.getByRole('button', { name: 'Registrar cliente nuevo' }));
      await llenarFormulario({
        documento: '71234568',
        nombre: 'Taller de prueba',
        telefono: '12345',
        correo: 'correo-invalido',
      });
      await userEvent.click(screen.getByRole('button', { name: 'Registrar cliente' }));

      expect(await screen.findByText('El teléfono tiene 9 dígitos.')).toBeInTheDocument();
      expect(
        screen.getByText('Ingresar un correo válido, por ejemplo nombre@dominio.pe.'),
      ).toBeInTheDocument();
    },
    TIEMPO_FORMULARIO,
  );

  it(
    'informa un documento repetido y ofrece seleccionar al cliente existente',
    async () => {
      render(<Selector />);

      await userEvent.click(screen.getByRole('button', { name: 'Registrar cliente nuevo' }));
      await llenarFormulario({ documento: '41234567', nombre: 'Otro nombre' });
      await userEvent.click(screen.getByRole('button', { name: 'Registrar cliente' }));

      expect(
        await screen.findByText('Ya existe un cliente con este documento.'),
      ).toBeInTheDocument();
      await userEvent.click(screen.getByRole('button', { name: 'Seleccionar cliente existente' }));

      expect(screen.getByTestId('elegido')).toHaveTextContent('CLI-003');
    },
    TIEMPO_FORMULARIO,
  );

  it('permite continuar sin cliente', async () => {
    render(<Selector />);

    await elegirOpcion('Cliente', 'Continuar sin cliente');

    expect(screen.getByTestId('elegido')).toHaveTextContent(SIN_CLIENTE);
  });

  it('muestra los clientes inactivos deshabilitados con la causa', async () => {
    render(<Selector />);

    await userEvent.click(screen.getByRole('combobox', { name: 'Cliente' }));
    const inactivo = await screen.findByRole('option', {
      name: 'Constructora Misti S.R.L. (20890123456), inactivo: no se puede seleccionar',
      hidden: true,
    });
    expect(inactivo).toHaveAttribute('aria-disabled', 'true');

    await userEvent.click(inactivo);
    expect(screen.getByTestId('elegido')).toHaveTextContent('ninguno');
  });
});

describe('ClientesPage', () => {
  it(
    'registra un cliente desde la lista y aparece en activos',
    async () => {
      renderApp('/clientes', 'Vendedor');

      await userEvent.click(await screen.findByRole('button', { name: 'Registrar cliente' }));
      const dialogo = await screen.findByRole('dialog', { name: 'Registrar cliente' });
      await llenarFormulario({
        tipo: 'DNI',
        documento: '46112233',
        nombre: 'Ana Quispe Rojas',
        telefono: '988777666',
        correo: 'aquispe@correo.example',
      });
      await userEvent.click(within(dialogo).getByRole('button', { name: 'Registrar cliente' }));

      expect(await screen.findByText('Ana Quispe Rojas')).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /Activos/ })).toBeInTheDocument();
    },
    TIEMPO_FORMULARIO,
  );

  it('busca por nombre o por documento', async () => {
    renderApp('/clientes', 'Vendedor');

    await screen.findByText('Constructora Los Andes S.A.C.');
    await userEvent.type(screen.getByRole('searchbox', { name: 'Buscar cliente' }), 'Roberto');
    expect(screen.getByText('Roberto Salas Ccama')).toBeInTheDocument();
    expect(screen.queryByText('Constructora Los Andes S.A.C.')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Limpiar búsqueda' }));
    await userEvent.type(screen.getByRole('searchbox', { name: 'Buscar cliente' }), '20456789012');
    expect(screen.getByText('Constructora Los Andes S.A.C.')).toBeInTheDocument();
  });

  it(
    'desactiva un cliente tras confirmar y lo muestra en inactivos',
    async () => {
      renderApp('/clientes', 'Vendedor');

      await userEvent.click(await screen.findByText('Roberto Salas Ccama'));
      await userEvent.click(await screen.findByRole('button', { name: 'Desactivar cliente' }));
      const confirmar = await screen.findByRole('dialog', {
        name: 'Desactivar al cliente Roberto Salas Ccama',
      });
      await userEvent.click(within(confirmar).getByRole('button', { name: 'Desactivar cliente' }));

      await waitFor(() =>
        expect(screen.queryByText('Roberto Salas Ccama')).not.toBeInTheDocument(),
      );
      await userEvent.click(screen.getByRole('tab', { name: /Inactivos/ }));
      expect(await screen.findByText('Roberto Salas Ccama')).toBeInTheDocument();
    },
    TIEMPO_FORMULARIO,
  );

  it('no permite el acceso a un Almacenero', async () => {
    renderApp('/clientes', 'Almacenero');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Disponibilidad' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Clientes' })).not.toBeInTheDocument();
  });

  it('permite el acceso a un Vendedor', async () => {
    renderApp('/clientes', 'Vendedor');

    expect(await screen.findByRole('heading', { level: 1, name: 'Clientes' })).toBeInTheDocument();
  });

  it('la lista no tiene violaciones de accesibilidad', async () => {
    const { container } = renderApp('/clientes', 'Vendedor');

    await screen.findByText('Constructora Los Andes S.A.C.');
    expect(await axe(container, sinContraste)).toHaveNoViolations();
  });

  it('el diálogo de registro no tiene violaciones de accesibilidad', async () => {
    render(
      <AppProviders>
        <ClienteFormModal open onClose={vi.fn()} />
      </AppProviders>,
    );

    await screen.findByRole('dialog', { name: 'Registrar cliente' });
    expect(
      await axe(document.body, {
        rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
      }),
    ).toHaveNoViolations();
  });
});
