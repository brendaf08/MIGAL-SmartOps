import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { describe, expect, it, vi } from 'vitest';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { AppProviders } from '@/app/AppProviders';
import { FormPage } from './FormPage';

expect.extend(matchers);

function Demo({
  onGuardar = vi.fn(),
  onCancel = vi.fn(),
  reason,
  canSubmit = true,
}: {
  onGuardar?: (v: { nombre: string }) => void;
  onCancel?: () => void;
  reason?: string;
  canSubmit?: boolean;
}) {
  const form = useForm({
    initialValues: { nombre: '' },
    validate: { nombre: (v) => (v.trim() ? null : 'Ingresar el nombre.') },
  });
  return (
    <AppProviders>
      <FormPage
        title="Registrar usuario"
        submitLabel="Guardar usuario"
        summary="1 campo obligatorio"
        reason={reason}
        canSubmit={canSubmit}
        onSubmit={form.onSubmit(onGuardar)}
        onCancel={onCancel}
      >
        <TextInput label="Nombre" {...form.getInputProps('nombre')} />
      </FormPage>
    </AppProviders>
  );
}

describe('FormPage', () => {
  it('envía el formulario válido y cancela con el botón Cancelar', async () => {
    const onGuardar = vi.fn();
    const onCancel = vi.fn();
    render(<Demo onGuardar={onGuardar} onCancel={onCancel} />);

    await userEvent.type(screen.getByRole('textbox', { name: 'Nombre' }), 'Ana');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar usuario' }));
    expect(onGuardar).toHaveBeenCalledWith({ nombre: 'Ana' }, expect.anything());

    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(onCancel).toHaveBeenCalledOnce();
  });

  it('con errores no envía, muestra el mensaje y enfoca el primer campo inválido', async () => {
    const onGuardar = vi.fn();
    render(<Demo onGuardar={onGuardar} />);

    await userEvent.click(screen.getByRole('button', { name: 'Guardar usuario' }));

    expect(onGuardar).not.toHaveBeenCalled();
    expect(await screen.findByText('Ingresar el nombre.')).toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole('textbox', { name: 'Nombre' })).toHaveFocus());
  });

  it('deshabilita la acción principal y explica el motivo', () => {
    render(<Demo canSubmit={false} reason="Falta completar el nombre." />);

    expect(screen.getByRole('button', { name: 'Guardar usuario' })).toBeDisabled();
    expect(screen.getByRole('status')).toHaveTextContent('Falta completar el nombre.');
  });

  it('no tiene violaciones de accesibilidad', async () => {
    const { container } = render(<Demo reason="Falta completar el nombre." canSubmit={false} />);

    expect(
      await axe(container, {
        rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
      }),
    ).toHaveNoViolations();
  });
});
