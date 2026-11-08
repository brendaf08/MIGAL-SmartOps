import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { AppProviders } from '@/app/AppProviders';
import { Pagination } from './Pagination';

const renderPagination = (page: number, onChange = vi.fn()) => {
  render(
    <AppProviders>
      <Pagination page={page} total={238} onChange={onChange} />
    </AppProviders>,
  );
  return onChange;
};

/* jsdom aplica las reglas @media de Mantine sin evaluarlas: las variantes responsive se consultan con `hidden`. */
describe('Pagination', () => {
  it('resume el rango de registros y el número de página', () => {
    renderPagination(3);

    expect(screen.getByText(/Mostrando 41–60 de 238/)).toBeInTheDocument();
    expect(screen.getByText('Página 3 de 12')).toBeInTheDocument();
  });

  it('deshabilita las flechas en los extremos y cambia de página', async () => {
    const onChange = renderPagination(1);

    expect(screen.getByRole('button', { name: 'Página anterior', hidden: true })).toBeDisabled();
    await userEvent.click(screen.getByRole('button', { name: 'Página siguiente', hidden: true }));
    expect(onChange).toHaveBeenCalledWith(2);
  });
});
