import type { NavGroup } from '@/components/layout/AppShell/NavMenu';

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
      {
        key: 'alertas',
        label: 'Alertas de stock',
        icon: 'alert-triangle',
        roles: ['Almacenero', 'Reponedor'],
      },
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
      { key: 'cotizaciones', label: 'Cotizaciones', icon: 'cotizacion', roles: ['Vendedor'] },
      { key: 'ventas', label: 'Ventas', icon: 'venta', roles: ['Vendedor'] },
      { key: 'clientes', label: 'Clientes', icon: 'users', roles: ['Vendedor'] },
    ],
  },
  {
    label: 'Abastecimiento',
    items: [
      { key: 'proveedores', label: 'Proveedores', icon: 'proveedor', roles: ['Reponedor'] },
      { key: 'compras', label: 'Órdenes de compra', icon: 'orden-compra', roles: ['Reponedor'] },
    ],
  },
  { label: 'Asistente', items: [{ key: 'agente-ia', label: 'Agente de IA', icon: 'sparkles' }] },
  {
    label: 'Administración',
    items: [{ key: 'usuarios', label: 'Usuarios y roles', icon: 'rol', roles: ['Administrador'] }],
  },
];
