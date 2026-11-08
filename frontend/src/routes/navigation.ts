import type { NavGroup } from '@/components/layout/AppShell/NavMenu';

export type Role = 'Administrador' | 'Vendedor' | 'Almacenero' | 'Reponedor';

/**
 * Navegación por módulo y rol, derivada de los actores de los casos de uso (docs/diseno/sistema-de-diseno.md).
 * Es una ayuda de usabilidad: la autorización real la aplica el backend. Pendiente de confirmar si el Administrador
 * accede a todos los módulos.
 */
export const NAV: NavGroup[] = [
  {
    label: 'Operación',
    items: [
      {
        key: 'disponibilidad',
        label: 'Disponibilidad',
        icon: 'producto',
        roles: ['Almacenero', 'Vendedor'],
      },
      { key: 'catalogo', label: 'Catálogo', icon: 'sku', roles: ['Almacenero', 'Administrador'] },
      { key: 'kardex', label: 'Movimientos y Kardex', icon: 'kardex', roles: ['Almacenero'] },
      { key: 'despachos', label: 'Despachos', icon: 'despacho', roles: ['Almacenero'] },
      { key: 'recepciones', label: 'Recepciones', icon: 'recepcion', roles: ['Almacenero'] },
      {
        key: 'documentos',
        label: 'Documentos OCR',
        icon: 'scan',
        roles: ['Vendedor', 'Almacenero'],
      },
    ],
  },
  {
    label: 'Comercial',
    items: [
      { key: 'ventas', label: 'Cotizaciones y ventas', icon: 'cotizacion', roles: ['Vendedor'] },
    ],
  },
  {
    label: 'Abastecimiento',
    items: [
      {
        key: 'proveedores',
        label: 'Proveedores y compras',
        icon: 'proveedor',
        roles: ['Reponedor'],
      },
    ],
  },
  { label: 'Asistente', items: [{ key: 'agente-ia', label: 'Agente de IA', icon: 'sparkles' }] },
  {
    label: 'Administración',
    items: [{ key: 'usuarios', label: 'Usuarios y roles', icon: 'rol', roles: ['Administrador'] }],
  },
];
