import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';

afterEach(() => window.sessionStorage.clear());

describe('Documentos imprimibles', () => {
  it('la cotización se imprime en su propia hoja, con totales y sin la navegación', async () => {
    renderApp('/cotizaciones/COT-0231/imprimir', 'Vendedor');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Cotización COT-0231' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Imprimir' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Volver a la cotización' })).toBeInTheDocument();
    expect(screen.getByRole('rowheader', { name: 'Total' })).toBeInTheDocument();
  });

  it('la orden de compra tiene su hoja imprimible', async () => {
    renderApp('/compras/OC-0087/imprimir', 'Reponedor');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Orden de compra OC-0087' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('rowheader', { name: 'Total de la orden' })).toBeInTheDocument();
  });
});
