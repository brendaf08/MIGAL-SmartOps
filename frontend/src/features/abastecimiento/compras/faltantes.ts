import type { FaltanteVenta, OrigenFaltante } from '@/features/inventario/api';

/** Líneas de una venta pendiente de abastecimiento: lo que falta de cada producto para esa venta. */
export interface VentaConFaltantes {
  ventaId: string;
  ventaCodigo: string;
  cotizacionId: string;
  cotizacionCodigo: string;
  cliente: string;
  fechaVenta: string;
  fechaEntregaRequerida: string | null;
  productos: { sku: string; nombre: string; unidad: string; faltante: number }[];
}

export type Ventana = 'todas' | '7' | '15' | '30' | 'sin-fecha';

export const VENTANAS: { value: Ventana; label: string }[] = [
  { value: 'todas', label: 'Todas las fechas' },
  { value: '7', label: 'Próximos 7 días' },
  { value: '15', label: 'Próximos 15 días' },
  { value: '30', label: 'Próximos 30 días' },
  { value: 'sin-fecha', label: 'Sin fecha de entrega' },
];

/** Días desde `hoy` hasta la fecha (negativo si ya pasó); `null` sin fecha. */
export function diasHasta(fecha: string | null, hoy: string): number | null {
  if (!fecha) return null;
  const dia = 86_400_000;
  return Math.round((Date.parse(`${fecha}T00:00:00Z`) - Date.parse(`${hoy}T00:00:00Z`)) / dia);
}

/** Indica si la fecha de entrega cae dentro de la ventana elegida; lo vencido siempre se incluye por urgente. */
export function dentroDeVentana(fecha: string | null, ventana: Ventana, hoy: string): boolean {
  if (ventana === 'todas') return true;
  if (ventana === 'sin-fecha') return fecha === null;
  const dias = diasHasta(fecha, hoy);
  return dias !== null && dias <= Number(ventana);
}

const nuevaVenta = (o: OrigenFaltante): VentaConFaltantes => ({
  ventaId: o.ventaId,
  ventaCodigo: o.ventaCodigo,
  cotizacionId: o.cotizacionId,
  cotizacionCodigo: o.cotizacionCodigo,
  cliente: o.cliente,
  fechaVenta: o.fechaVenta,
  fechaEntregaRequerida: o.fechaEntregaRequerida,
  productos: [],
});

/** Reagrupa los faltantes por producto en faltantes por venta, de la entrega más próxima a la más lejana. */
export function agruparPorVenta(
  items: FaltanteVenta[],
  ventana: Ventana,
  hoy: string,
): VentaConFaltantes[] {
  const ventas = new Map<string, VentaConFaltantes>();
  for (const producto of items) {
    for (const origen of producto.origenes) {
      if (!dentroDeVentana(origen.fechaEntregaRequerida, ventana, hoy)) continue;
      const venta = ventas.get(origen.ventaId) ?? nuevaVenta(origen);
      venta.productos.push({
        sku: producto.sku,
        nombre: producto.nombre,
        unidad: producto.unidad,
        faltante: origen.faltante,
      });
      ventas.set(origen.ventaId, venta);
    }
  }
  return [...ventas.values()].sort(
    (a, b) =>
      (a.fechaEntregaRequerida ?? '9999').localeCompare(b.fechaEntregaRequerida ?? '9999') ||
      a.ventaCodigo.localeCompare(b.ventaCodigo),
  );
}

/** Faltantes por producto considerando solo las ventas de la ventana elegida; recalcula lo que falta pedir. */
export function filtrarPorProducto(
  items: FaltanteVenta[],
  ventana: Ventana,
  hoy: string,
): FaltanteVenta[] {
  return items
    .map((p) => {
      const origenes = p.origenes.filter((o) =>
        dentroDeVentana(o.fechaEntregaRequerida, ventana, hoy),
      );
      const faltante = origenes.reduce((suma, o) => suma + o.faltante, 0);
      return {
        ...p,
        origenes,
        faltante,
        ventas: origenes.length,
        porCubrir: Math.max(faltante - p.planificado, 0),
      };
    })
    .filter((p) => p.origenes.length > 0);
}
