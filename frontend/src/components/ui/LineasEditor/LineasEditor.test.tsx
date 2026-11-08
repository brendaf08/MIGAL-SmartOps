import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { AppProviders } from '@/app/AppProviders';
import { LineasEditor } from './LineasEditor';
import {
  descuentoValido,
  siguienteIdLinea,
  subtotalLineas,
  totalLineas,
  type LineaEditable,
} from './calculos';

expect.extend(matchers);

const productos = [
  { sku: 'CEM-001', nombre: 'Cemento Sol tipo I' },
  { sku: 'FIE-003', nombre: 'Fierro corrugado 1/2 in' },
];
const inicial: LineaEditable[] = [{ id: 1, sku: 'CEM-001', cantidad: 10, precio: 28.9 }];

function Demo({ conDescuento = false }: { conDescuento?: boolean }) {
  const [lineas, setLineas] = useState(inicial);
  const [descuento, setDescuento] = useState(0);
  return (
    <AppProviders>
      <LineasEditor
        lineas={lineas}
        onChange={setLineas}
        productos={productos}
        conPrecio
        conDescuento={conDescuento}
        descuento={descuento}
        onDescuentoChange={setDescuento}
      />
    </AppProviders>
  );
}

describe('cálculos de líneas', () => {
  const lineas: LineaEditable[] = [
    { id: 1, sku: 'A', cantidad: 3, precio: 10.5 },
    { id: 2, sku: 'B', cantidad: 2, precio: 0.1 },
  ];

  it('suma subtotales con dos decimales y aplica el descuento sin pasar de cero', () => {
    expect(subtotalLineas(lineas)).toBe(31.7);
    expect(totalLineas(lineas, 1.7)).toBe(30);
    expect(totalLineas(lineas, 99)).toBe(0);
  });

  it('valida el descuento contra el subtotal', () => {
    expect(descuentoValido(lineas, 31.7)).toBe(true);
    expect(descuentoValido(lineas, 31.71)).toBe(false);
    expect(descuentoValido(lineas, -1)).toBe(false);
  });

  it('calcula el siguiente identificador de línea', () => {
    expect(siguienteIdLinea([])).toBe(1);
    expect(siguienteIdLinea(lineas)).toBe(3);
  });
});

describe('LineasEditor', () => {
  it('muestra el subtotal y el total de las líneas', () => {
    render(<Demo />);

    const tabla = screen.getByRole('table', { name: 'Productos del documento' });
    expect(within(tabla).getAllByText(/S\/\s289\.00/).length).toBeGreaterThanOrEqual(2);
  });

  it('agrega y quita líneas', async () => {
    render(<Demo />);

    await userEvent.click(screen.getByRole('button', { name: 'Agregar producto' }));
    expect(screen.getByRole('button', { name: 'Quitar la línea 2' })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Quitar la línea 2' }));
    expect(screen.queryByRole('button', { name: 'Quitar la línea 2' })).not.toBeInTheDocument();
  });

  it('muestra el descuento y lo resta del total', async () => {
    render(<Demo conDescuento />);

    const descuento = screen.getByRole('textbox', { name: 'Descuento en soles' });
    await userEvent.clear(descuento);
    await userEvent.type(descuento, '9');

    expect(await screen.findAllByText(/S\/\s280\.00/)).not.toHaveLength(0);
  });

  it('no tiene violaciones de accesibilidad', async () => {
    const { container } = render(<Demo conDescuento />);

    expect(
      await axe(container, {
        rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
      }),
    ).toHaveNoViolations();
  });
});
