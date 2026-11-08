import { render, screen, within } from '@testing-library/react';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { describe, expect, it } from 'vitest';
import { AppProviders } from '@/app/AppProviders';
import { StockBar } from '../StockBar/StockBar';
import { StockLocations } from './StockLocations';

expect.extend(matchers);

const ubicaciones = [
  { nombre: 'Tienda principal', cantidad: 40 },
  { nombre: 'Almacén 1', cantidad: 300, reservado: 100 },
  { nombre: 'Almacén 2', cantidad: 0 },
];

describe('StockLocations', () => {
  it('muestra las ubicaciones en orden con su cantidad libre y marca la que no tiene stock', () => {
    render(
      <AppProviders>
        <StockLocations ubicaciones={ubicaciones} unidad="bolsa" />
      </AppProviders>,
    );

    const celdas = within(screen.getByRole('list', { name: 'Existencias por ubicación' })).getAllByRole(
      'listitem',
    );
    expect(celdas.map((c) => c.textContent)).toEqual([
      expect.stringContaining('Tienda principal'),
      expect.stringContaining('Almacén 1'),
      expect.stringContaining('Almacén 2'),
    ]);
    expect(celdas[0]).toHaveTextContent('40');
    expect(celdas[1]).toHaveTextContent('200');
    expect(celdas[2]).toHaveTextContent('Sin stock');
  });

  it('no tiene violaciones de accesibilidad en las variantes normal y compacta', async () => {
    const { container } = render(
      <AppProviders>
        <StockLocations ubicaciones={ubicaciones} />
        <StockLocations ubicaciones={ubicaciones} compact label="Existencias compactas" />
      </AppProviders>,
    );

    expect(
      await axe(container, { rules: { 'color-contrast': { enabled: false } } }),
    ).toHaveNoViolations();
  });
});

describe('StockBar ampliado', () => {
  it('incluye los marcadores de mínimo y reposición en el texto accesible y la leyenda', () => {
    render(
      <AppProviders>
        <StockBar libre={120} reservado={30} minimo={50} reposicion={80} unidad="bolsa" />
      </AppProviders>,
    );

    expect(screen.getByRole('img')).toHaveAccessibleName(/Mínimo 50, Reposición 80/);
    const items = screen.getAllByRole('listitem').map((li) => li.textContent);
    expect(items).toContain('Mínimo 50 bolsa');
    expect(items).toContain('Reposición 80 bolsa');
  });

  it('la variante compacta omite la leyenda y conserva la lectura exacta', () => {
    render(
      <AppProviders>
        <StockBar compact libre={120} reservado={30} minimo={50} label="Reparto" />
      </AppProviders>,
    );

    expect(screen.getByRole('img')).toHaveAccessibleName('Reparto: Libre 120, Reservado 30');
    expect(screen.queryByText('Libre')).not.toBeInTheDocument();
  });
});
