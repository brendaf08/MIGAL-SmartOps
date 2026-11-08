import type { CSSProperties } from 'react';
import { cx } from '@/lib/cx';
import { formatNumber } from '@/lib/format';
import classes from './StockLocations.module.css';

export interface StockLocation {
  nombre: string;
  /** Nombre para la variante compacta, donde el ancho de la celda es reducido. */
  nombreCorto?: string;
  /** Existencia física de la ubicación. */
  cantidad: number;
  /** Parte reservada de esa ubicación; el reservado de la API es consolidado, por lo que suele omitirse. */
  reservado?: number;
}

export interface StockLocationsProps {
  /** En el orden Tienda, Almacén 1, Almacén 2, para que la posición se aprenda. */
  ubicaciones: StockLocation[];
  unidad?: string;
  /** Existencia a partir de la cual una celda alcanza el ancho máximo; por omisión, la mayor de las ubicaciones. */
  escala?: number;
  /** Teléfono: tres celdas iguales en una fila, sin barra. */
  compact?: boolean;
  /** Texto accesible de la lista. */
  label?: string;
}

/** Plano esquemático de dónde está un producto: una celda por ubicación, con su cantidad libre. */
export function StockLocations({
  ubicaciones,
  unidad,
  escala,
  compact = false,
  label = 'Existencias por ubicación',
}: StockLocationsProps) {
  const max = Math.max(escala ?? Math.max(...ubicaciones.map((u) => u.cantidad), 0), 1);

  return (
    <ul className={cx(classes.root, compact && classes.compact)} aria-label={label}>
      {ubicaciones.map((u) => {
        const reservado = Math.min(u.reservado ?? 0, u.cantidad);
        const libre = u.cantidad - reservado;
        const vacia = u.cantidad <= 0;
        return (
          <li
            key={u.nombre}
            className={cx(classes.cell, vacia && classes.empty)}
            style={
              compact
                ? undefined
                : ({ flexGrow: Math.max(u.cantidad / max, 0.25) } as CSSProperties)
            }
          >
            <span className={classes.name}>{compact ? (u.nombreCorto ?? u.nombre) : u.nombre}</span>
            {vacia ? (
              <span className={classes.none}>Sin stock</span>
            ) : (
              <span className={classes.figure}>
                {formatNumber(libre)}
                {unidad && !compact && <small> {unidad}</small>}
                <span className={classes.sr}>
                  {u.reservado === undefined ? ' en existencia' : ' libres'}
                </span>
              </span>
            )}
            {!compact && !vacia && (
              <span className={classes.bar} aria-hidden="true">
                <span className={classes.free} style={{ flexGrow: libre }} />
                {reservado > 0 && <span className={classes.held} style={{ flexGrow: reservado }} />}
              </span>
            )}
          </li>
        );
      })}
    </ul>
  );
}
