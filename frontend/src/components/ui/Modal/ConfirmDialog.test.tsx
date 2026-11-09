import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { AppProviders } from '@/app/AppProviders';
import { ConfirmDialog } from './ConfirmDialog';

const renderDialog = (props: Partial<Parameters<typeof ConfirmDialog>[0]> = {}) => {
  const onConfirm = vi.fn();
  const onCancel = vi.fn();
  render(
    <AppProviders>
      <ConfirmDialog
        open
        title="Cancelar la venta V-0042"
        consequences={['Se liberará la reserva.']}
        confirmLabel="Cancelar venta"
        cancelLabel="Volver"
        tone="danger"
        onConfirm={onConfirm}
        onCancel={onCancel}
        {...props}
      />
    </AppProviders>,
  );
  return { onConfirm, onCancel };
};

describe('ConfirmDialog', () => {
  it('nombra la acción, lista las consecuencias y enfoca la acción segura', async () => {
    renderDialog();

    expect(
      await screen.findByRole('dialog', { name: 'Cancelar la venta V-0042' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Se liberará la reserva.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Volver' })).toHaveFocus();
  });

  it('confirma y cancela con sus botones', async () => {
    const { onConfirm, onCancel } = renderDialog();

    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar venta' }));
    await userEvent.click(screen.getByRole('button', { name: 'Volver' }));
    expect(onConfirm).toHaveBeenCalledTimes(1);
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('no permite cerrar mientras procesa', async () => {
    renderDialog({ loading: true });

    await screen.findByRole('dialog');
    expect(screen.queryByRole('button', { name: /close|cerrar/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Volver' })).toBeDisabled();
  });
});
