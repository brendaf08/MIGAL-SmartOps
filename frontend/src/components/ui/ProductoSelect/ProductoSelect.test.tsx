import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { AppProviders } from '@/app/AppProviders';
import { MoneyInput } from '../MoneyInput/MoneyInput';
import { ProductoSelect } from './ProductoSelect';

expect.extend(matchers);

const productos = [
  { sku: 'CEM-001', nombre: 'Cemento Sol tipo I' },
  { sku: 'FIE-003', nombre: 'Fierro corrugado 1/2 in' },
];

function Demo({ sugeridos }: { sugeridos?: string[] }) {
  const [sku, setSku] = useState<string | null>(null);
  return (
    <AppProviders>
      <ProductoSelect
        label="Producto"
        productos={productos}
        sugeridos={sugeridos}
        value={sku}
        onChange={setSku}
      />
      <output data-testid="elegido">{sku ?? 'ninguno'}</output>
      <MoneyInput label="Precio" defaultValue={1250} />
    </AppProviders>
  );
}

describe('ProductoSelect', () => {
  it('elige un producto de la lista por su etiqueta SKU, nombre', async () => {
    render(<Demo />);

    await userEvent.click(screen.getByRole('combobox', { name: 'Producto' }));
    await userEvent.click(
      await screen.findByRole('option', { name: 'FIE-003, Fierro corrugado 1/2 in', hidden: true }),
    );

    expect(screen.getByTestId('elegido')).toHaveTextContent('FIE-003');
  });

  it('ofrece primero los productos sugeridos', async () => {
    render(<Demo sugeridos={['FIE-003']} />);

    await userEvent.click(screen.getByRole('combobox', { name: 'Producto' }));
    const opciones = await screen.findAllByRole('option', { hidden: true });

    expect(opciones[0]).toHaveTextContent('FIE-003');
  });
});

describe('MoneyInput', () => {
  it('muestra el importe con prefijo, miles y dos decimales', () => {
    render(<Demo />);

    expect(screen.getByRole('textbox', { name: 'Precio' })).toHaveValue('S/ 1,250.00');
  });

  it('no tiene violaciones de accesibilidad junto al selector', async () => {
    const { container } = render(<Demo />);

    expect(
      await axe(container, {
        rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
      }),
    ).toHaveNoViolations();
  });
});
