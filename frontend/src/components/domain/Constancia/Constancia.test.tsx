import { render, screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { AppProviders } from '@/app/AppProviders';
import { StatusBadge } from '../StatusBadge/StatusBadge';
import { Constancia, type CambioStock } from './Constancia';

expect.extend(matchers);

const cambios: CambioStock[] = [
  {
    producto: 'Cemento Sol tipo I',
    ubicacion: 'Almacén 1',
    antes: 1200,
    despues: 1300,
    unidad: 'bolsa',
  },
];

const montar = (props: Partial<Parameters<typeof Constancia>[0]> = {}) =>
  render(
    <AppProviders>
      <Constancia
        sello={<StatusBadge entity="recepcion" value="Validada" variant="sello" />}
        title="Recepción R-0013 registrada"
        description="Orden OC-0087."
        cambios={cambios}
        nota="Las entradas quedaron registradas en el Kardex."
        actions={<button type="button">Ver movimientos en el Kardex</button>}
        {...props}
      />
    </AppProviders>,
  );

afterEach(() => {
  vi.restoreAllMocks();
  Reflect.deleteProperty(navigator, 'vibrate');
});

describe('Constancia', () => {
  it('se anuncia como estado y muestra la existencia antes y después', () => {
    montar();

    const constancia = screen.getByRole('status', { name: 'Recepción R-0013 registrada' });
    const tabla = within(constancia).getByRole('table', { name: 'Existencia antes y después' });
    expect(within(tabla).getByRole('row', { name: /Cemento Sol tipo I/ })).toHaveTextContent(
      '1,200 bolsa1,300 bolsa',
    );
    expect(constancia).toHaveTextContent('Las entradas quedaron registradas en el Kardex.');
  });

  it('vibra 30 ms al montarse en un dispositivo que lo admite', () => {
    const vibrate = vi.fn();
    Object.defineProperty(navigator, 'vibrate', { value: vibrate, configurable: true });

    montar();

    expect(vibrate).toHaveBeenCalledExactlyOnceWith(30);
  });

  it('no vibra con movimiento reducido ni cuando se desactiva', () => {
    const vibrate = vi.fn();
    Object.defineProperty(navigator, 'vibrate', { value: vibrate, configurable: true });
    const original = window.matchMedia;
    window.matchMedia = vi.fn().mockReturnValue({
      matches: true,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
    });

    montar();
    expect(vibrate).not.toHaveBeenCalled();

    window.matchMedia = original;
    montar({ vibrate: false });
    expect(vibrate).not.toHaveBeenCalled();
  });

  it('no tiene violaciones de accesibilidad', async () => {
    const { container } = montar();

    expect(
      await axe(container, { rules: { 'color-contrast': { enabled: false } } }),
    ).toHaveNoViolations();
  });
});
