import type { Role } from './roles';
import { useSession } from './session';

/**
 * Matriz única de acciones de escritura por rol, copiada de la extensión `x-roles` de cada operación del contrato
 * (backend/api/modulos). La interfaz oculta una acción a quien el backend se la rechazaría con 403; la autorización real
 * la sigue aplicando el backend. Al cambiar `x-roles` en el contrato, actualizar aquí la entrada correspondiente.
 */
export const PERMISOS = {
  /** POST /cotizaciones, PUT /cotizaciones/{id}. */
  registrarCotizacion: ['Vendedor'],
  /** POST /ventas (desde cotización) o POST /ventas/directas. */
  registrarVenta: ['Vendedor', 'Administrador'],
  /** POST /ventas/{id}/cobertura. */
  completarVenta: ['Almacenero', 'Vendedor'],
  /** POST /ventas/{id}/cancelacion. */
  cancelarVenta: ['Vendedor'],
  /** POST /recepciones. */
  registrarRecepcion: ['Almacenero'],
  /** POST /guias-despacho, /salida, /reimpresiones y /entrega. */
  operarDespacho: ['Almacenero'],
  /** POST /inventario/movimientos y /inventario/traslados. */
  registrarMovimiento: ['Almacenero', 'Administrador'],
  /** POST /ordenes-compra, PUT y cierre. */
  registrarCompra: ['Reponedor'],
} as const satisfies Record<string, readonly Role[]>;

export type Accion = keyof typeof PERMISOS;

/** Indica si el rol puede ejecutar la acción. */
export const permite = (rol: Role | undefined, accion: Accion): boolean =>
  rol !== undefined && (PERMISOS[accion] as readonly Role[]).includes(rol);

/** Función `puede(accion)` para el usuario de la sesión. */
export function usePermisos() {
  const { user } = useSession();
  return (accion: Accion) => permite(user?.role, accion);
}
