import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';

afterEach(() => window.sessionStorage.clear());

describe('AgenteIAPage', () => {
  it('muestra el rol del usuario y responde con las consultas realizadas', async () => {
    renderApp('/agente-ia', 'Vendedor');

    expect(await screen.findByText('Rol: Vendedor')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('Consulta al agente'), 'cemento sol{Enter}');

    expect(
      await screen.findByText(/Consultó disponibilidad de CEM-001/, undefined, { timeout: 3000 }),
    ).toBeInTheDocument();
    expect(screen.getByText(/libres de Cemento Sol/)).toBeInTheDocument();
  });

  it('marca como incierta la respuesta sin información y no inventa cantidades', async () => {
    renderApp('/agente-ia', 'Almacenero');

    await userEvent.type(await screen.findByLabelText('Consulta al agente'), 'zzzz yyyy{Enter}');

    expect(
      await screen.findByText(/No encontré información/, undefined, { timeout: 3000 }),
    ).toBeInTheDocument();
  });

  it('envía con el botón solo cuando hay texto', async () => {
    renderApp('/agente-ia', 'Almacenero');

    expect(await screen.findByRole('button', { name: 'Enviar consulta' })).toBeDisabled();
  });
});
