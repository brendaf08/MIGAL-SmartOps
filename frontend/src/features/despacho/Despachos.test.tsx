import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { ORDENES } from './fixtures';

afterEach(() => window.sessionStorage.clear());

describe('Despachos', () => {
  it('abre la orden desde el listado', async () => {
    renderApp('/despachos', 'Almacenero');

    await userEvent.click(await screen.findByText('D-0031'));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Orden de despacho D-0031' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Todas las cantidades están reservadas')).toBeInTheDocument();
  });

  it('no permite confirmar la salida si hay cantidades sin reservar', async () => {
    renderApp('/despachos/D-0030', 'Almacenero');

    expect(await screen.findByText('Hay cantidades sin reservar')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Confirmar salida' })).toBeDisabled();
  });

  it('confirma la salida solo tras el diálogo, que enfoca Volver y enuncia el Kardex', async () => {
    renderApp('/despachos/D-0031', 'Almacenero');

    await userEvent.click(await screen.findByRole('button', { name: 'Confirmar salida' }));
    const dialog = await screen.findByRole('dialog', { name: 'Confirmar salida de la mercadería' });
    expect(screen.getByRole('button', { name: 'Volver' })).toHaveFocus();
    expect(dialog).toHaveTextContent('movimientos de salida en el Kardex');

    await userEvent.click(within(dialog).getByRole('button', { name: 'Confirmar salida' }));

    const constancia = await screen.findByRole('status', {
      name: 'Salida de la orden D-0031 confirmada',
    });
    expect(constancia).toHaveTextContent('Se registraron las salidas de la orden D-0031');
    expect(
      within(constancia).getByRole('table', { name: 'Existencia antes y después' }),
    ).toBeInTheDocument();
    // La tabla de origen refleja el cambio: la orden ya no se puede confirmar de nuevo.
    await waitFor(() =>
      expect(screen.queryByRole('button', { name: 'Confirmar salida' })).not.toBeInTheDocument(),
    );
    expect(ORDENES[0]!.estado).toBe('SalidaConfirmada');
    ORDENES[0]!.estado = 'Emitida';
  });
});
