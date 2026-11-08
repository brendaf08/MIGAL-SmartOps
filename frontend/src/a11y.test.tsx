import { screen, waitFor } from '@testing-library/react';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { afterEach, describe, expect, it } from 'vitest';
import type { Role } from '@/features/seguridad/roles';
import { renderApp } from '@/test/renderApp';

expect.extend(matchers);
afterEach(() => window.sessionStorage.clear());

/** Pantalla, rol y el texto que indica que ya cargó. El contraste se verifica con los tokens, no en jsdom. */
const PANTALLAS: [ruta: string, rol: Role | undefined, listo: RegExp | string][] = [
  ['/acceso', undefined, 'Iniciar sesión'],
  ['/disponibilidad', 'Almacenero', 'CEM-001'],
  ['/cotizaciones/COT-0231', 'Vendedor', 'Esta cotización no reserva existencias'],
  ['/despachos/D-0031', 'Almacenero', 'Todas las cantidades están reservadas'],
  ['/recepciones', 'Almacenero', 'R-0012'],
  ['/agente-ia', 'Vendedor', 'Rol: Vendedor'],
  ['/kardex', 'Almacenero', '−200'],
  ['/catalogo', 'Administrador', 'CEM-001'],
  ['/proveedores', 'Reponedor', 'Distribuidora Andina S.A.C.'],
  ['/usuarios', 'Administrador', 'rquispe'],
];

describe('Accesibilidad (axe)', () => {
  it.each(PANTALLAS)('%s no tiene violaciones', async (ruta, rol, listo) => {
    const { container } = renderApp(ruta, rol);

    await waitFor(() => expect(screen.getAllByText(listo).length).toBeGreaterThan(0), {
      timeout: 3000,
    });

    expect(
      await axe(container, { rules: { 'color-contrast': { enabled: false } } }),
    ).toHaveNoViolations();
  });
});
