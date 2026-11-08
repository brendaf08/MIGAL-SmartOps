import { screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import type { Role } from '@/features/seguridad/roles';
import { NAV } from '@/routes/navigation';
import { renderApp } from '@/test/renderApp';

expect.extend(matchers);
afterEach(() => window.sessionStorage.clear());

/** Todas las pantallas de la aplicación con un rol que puede verlas. Las rutas con identificador usan datos de prueba. */
const RUTAS: [ruta: string, rol: Role][] = [
  ['/usuarios', 'Administrador'],
  ['/usuarios/nuevo', 'Administrador'],
  ['/catalogo', 'Administrador'],
  ['/catalogo/nuevo', 'Administrador'],
  ['/catalogo/CEM-001', 'Administrador'],
  ['/catalogo/CEM-001/editar', 'Administrador'],
  ['/alertas', 'Reponedor'],
  ['/kardex', 'Almacenero'],
  ['/kardex/nuevo', 'Almacenero'],
  ['/despachos', 'Almacenero'],
  ['/despachos/D-0031', 'Almacenero'],
  ['/despachos/D-0031/imprimir', 'Almacenero'],
  ['/recepciones', 'Almacenero'],
  ['/recepciones/nueva', 'Almacenero'],
  ['/documentos', 'Vendedor'],
  ['/documentos/procesar', 'Vendedor'],
  ['/cotizaciones', 'Vendedor'],
  ['/cotizaciones/nueva', 'Vendedor'],
  ['/cotizaciones/COT-0231', 'Vendedor'],
  ['/cotizaciones/COT-0231/editar', 'Vendedor'],
  ['/ventas', 'Vendedor'],
  ['/ventas/nueva', 'Vendedor'],
  ['/ventas/V-0040', 'Vendedor'],
  ['/clientes', 'Vendedor'],
  ['/proveedores', 'Reponedor'],
  ['/proveedores/nueva', 'Reponedor'],
  ['/proveedores/P-001', 'Reponedor'],
  ['/compras', 'Reponedor'],
  ['/compras/nueva', 'Reponedor'],
  ['/compras/OC-0087', 'Reponedor'],
];

describe('Accesibilidad de todas las pantallas (axe)', () => {
  it.each(RUTAS)(
    '%s no tiene violaciones para el rol %s',
    async (ruta, rol) => {
      const { container } = renderApp(ruta, rol);

      await screen.findByRole('heading', { level: 1 }, { timeout: 8000 });
      await waitFor(() =>
        expect(screen.queryByText('Módulo en construcción')).not.toBeInTheDocument(),
      );
      await waitFor(() => expect(container.querySelector('[class*="Skeleton"]')).toBeNull(), {
        timeout: 8000,
      });

      expect(
        await axe(container, {
          rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
        }),
      ).toHaveNoViolations();
    },
    30000,
  );
});

describe('Cobertura de la navegación', () => {
  const modulos = NAV.flatMap((grupo) => grupo.items).filter((item) => item.key !== 'agente-ia');

  it.each(modulos.map((m) => [m.key, m.roles?.[0] ?? 'Administrador'] as [string, Role]))(
    'el módulo %s abre su pantalla principal y no queda en construcción',
    async (clave, rol) => {
      renderApp(`/${clave}`, rol);

      await screen.findByRole('heading', { level: 1 }, { timeout: 8000 });

      expect(screen.queryByText('Módulo en construcción')).not.toBeInTheDocument();
    },
    20000,
  );

  it('cada rol ve en la navegación solo los módulos que le corresponden', async () => {
    const esperados: Record<Role, string[]> = {
      Administrador: ['Catálogo', 'Agente de IA', 'Usuarios y roles'],
      Vendedor: [
        'Disponibilidad',
        'Documentos OCR',
        'Cotizaciones',
        'Ventas',
        'Clientes',
        'Agente de IA',
      ],
      Almacenero: [
        'Disponibilidad',
        'Catálogo',
        'Alertas de stock',
        'Movimientos y Kardex',
        'Despachos',
        'Recepciones',
        'Documentos OCR',
        'Órdenes de compra',
        'Agente de IA',
      ],
      Reponedor: ['Alertas de stock', 'Proveedores', 'Órdenes de compra', 'Agente de IA'],
    };
    for (const [rol, etiquetas] of Object.entries(esperados) as [Role, string[]][]) {
      const visibles = NAV.flatMap((g) => g.items)
        .filter((i) => !i.roles || i.roles.includes(rol))
        .map((i) => i.label);
      expect(visibles.sort(), `módulos del rol ${rol}`).toEqual([...etiquetas].sort());
    }
  });
});
