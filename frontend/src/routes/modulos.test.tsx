import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import type { Role } from '@/features/seguridad/roles';
import { renderApp } from '@/test/renderApp';

afterEach(() => window.sessionStorage.clear());

/** Cada módulo sin flujo propio abre su listado con el encabezado del módulo y su tabla. */
const MODULOS: [ruta: string, rol: Role, titulo: string, tabla: string][] = [
  ['/catalogo', 'Administrador', 'Catálogo', 'Catálogo de productos'],
  ['/kardex', 'Almacenero', 'Movimientos y Kardex', 'Movimientos de Kardex'],
  ['/documentos', 'Vendedor', 'Documentos OCR', 'Documentos procesados'],
  ['/proveedores', 'Reponedor', 'Proveedores y compras', 'Proveedores'],
  ['/usuarios', 'Administrador', 'Usuarios y roles', 'Usuarios'],
];

describe('Módulos de listado', () => {
  it.each(MODULOS)('%s se muestra al rol %s', async (ruta, rol, titulo, tabla) => {
    renderApp(ruta, rol);

    expect(await screen.findByRole('heading', { level: 1, name: titulo })).toBeInTheDocument();
    expect(screen.getByRole('table', { name: tabla })).toBeInTheDocument();
  });

  it('el Kardex rotula cada movimiento con su signo y su origen', async () => {
    renderApp('/kardex', 'Almacenero');

    expect(await screen.findByText('−200')).toBeInTheDocument();
    expect(screen.getAllByText('Traslado')).toHaveLength(2);
  });

  it('la vista de órdenes de compra se alcanza desde las pestañas', async () => {
    renderApp('/proveedores', 'Reponedor');

    (await screen.findByRole('tab', { name: /Órdenes de compra/ })).click();

    expect(await screen.findByRole('table', { name: 'Órdenes de compra' })).toBeInTheDocument();
  });
});
