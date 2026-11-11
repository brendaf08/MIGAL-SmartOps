import type { NavGroup } from '@/components/layout/AppShell/NavMenu';

/**
 * Navegación por módulo y rol, derivada de los actores de los casos de uso (docs/diseno/sistema-de-diseno.md).
 * Es una ayuda de usabilidad: la autorización real la aplica el backend. El Administrador accede a todos los módulos; en los
 * flujos operativos su acceso es de consulta, porque el backend reserva el registro a cada rol operativo.
 */
export const NAV: NavGroup[] = [
  {
    label: 'Operación',
    items: [
      {
        key: 'disponibilidad',
        label: 'Disponibilidad',
        icon: 'producto',
        roles: ['Almacenero', 'Vendedor', 'Administrador'],
      },
      { key: 'catalogo', label: 'Catálogo', icon: 'sku', roles: ['Almacenero', 'Administrador'] },
      {
        key: 'alertas',
        label: 'Alertas de stock',
        icon: 'alert-triangle',
        roles: ['Almacenero', 'Reponedor', 'Administrador'],
      },
      {
        key: 'kardex',
        label: 'Movimientos y Kardex',
        icon: 'kardex',
        roles: ['Almacenero', 'Administrador'],
      },
      {
        key: 'despachos',
        label: 'Despachos',
        icon: 'despacho',
        roles: ['Almacenero', 'Administrador'],
      },
      {
        key: 'recepciones',
        label: 'Recepciones',
        icon: 'recepcion',
        roles: ['Almacenero', 'Administrador'],
      },
      {
        key: 'documentos',
        label: 'Documentos OCR',
        icon: 'scan',
        roles: ['Vendedor', 'Almacenero', 'Administrador'],
      },
    ],
  },
  {
    label: 'Comercial',
    items: [
      {
        key: 'cotizaciones',
        label: 'Cotizaciones',
        icon: 'cotizacion',
        roles: ['Vendedor', 'Administrador'],
      },
      { key: 'ventas', label: 'Ventas', icon: 'venta', roles: ['Vendedor', 'Administrador'] },
      { key: 'clientes', label: 'Clientes', icon: 'users', roles: ['Vendedor', 'Administrador'] },
    ],
  },
  {
    label: 'Abastecimiento',
    items: [
      {
        key: 'proveedores',
        label: 'Proveedores',
        icon: 'proveedor',
        roles: ['Reponedor', 'Administrador'],
      },
      {
        key: 'compras',
        label: 'Órdenes de compra',
        icon: 'orden-compra',
        roles: ['Reponedor', 'Almacenero', 'Administrador'],
      },
    ],
  },
  { label: 'Asistente', items: [{ key: 'agente-ia', label: 'Agente de IA', icon: 'sparkles' }] },
  {
    label: 'Administración',
    items: [{ key: 'usuarios', label: 'Usuarios y roles', icon: 'rol', roles: ['Administrador'] }],
  },
];
