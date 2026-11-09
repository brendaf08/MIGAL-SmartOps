import type { CSSProperties } from 'react';
import { cx } from '@/lib/cx';
import { formatNumber } from '@/lib/format';
import classes from './StockBar.module.css';

export interface StockBarProps {
  libre: number;
  reservado: number;
  /** Cantidad vendida sin existencia; no es stock físico ni genera salidas de Kardex. */
  pendiente?: number;
  unidad?: string;
  /** Texto accesible de la barra. */
  label?: string;
  legend?: boolean;
  /** Trazo de 8 px sin regla, marcadores ni leyenda, para celdas de tabla. */
  compact?: boolean;
  /** Unidad de venta: las marcas de la regla caen en múltiplos de `paso` (cada 100 bolsas, cada millar). */
  empaque?: { paso: number; unidad: string };
  /** Marcador cuadrado del stock mínimo sobre la barra. */
  minimo?: number;
  /** Marcador circular del punto de reposición sobre la barra. */
  reposicion?: number;
}

/** Marcas más juntas que esta fracción de la escala se descartan para que la regla siga legible. */
const MIN_TICK_RATIO = 0.03;

/** Barra segmentada del reparto del stock: libre, reservado y pendiente de abastecimiento (con rayas). */
export function StockBar({
  libre,
  reservado,
  pendiente = 0,
  unidad,
  label = 'Distribución de stock',
  legend = true,
  compact = false,
  empaque,
  minimo,
  reposicion,
}: StockBarProps) {
  const segments = [
    { key: 'libre', value: libre, text: 'Libre' },
    { key: 'reservado', value: reservado, text: 'Reservado' },
    { key: 'pendiente', value: pendiente, text: 'Pendiente de abastecimiento' },
  ].filter((s) => s.value > 0 || s.key !== 'pendiente');
  const total = libre + reservado + pendiente;
  const u = unidad ? ` ${unidad}` : '';

  const marcadores = compact
    ? []
    : [
        { key: 'minimo', value: minimo, text: 'Mínimo' },
        { key: 'reposicion', value: reposicion, text: 'Reposición' },
      ].filter((m): m is { key: string; value: number; text: string } => m.value !== undefined);

  /** La escala se amplía si un marcador queda fuera de la existencia, para que siga visible. */
  const escala = Math.max(total, ...marcadores.map((m) => m.value), 1);
  const resto = escala - total;
  const tick =
    empaque && empaque.paso > 0 && empaque.paso / escala >= MIN_TICK_RATIO
      ? (empaque.paso / escala) * 100
      : 10;

  const lectura = [
    ...segments.map((s) => `${s.text} ${formatNumber(s.value)}`),
    ...marcadores.map((m) => `${m.text} ${formatNumber(m.value)}`),
  ].join(', ');

  return (
    <div className={cx(classes.root, compact && classes.compact)}>
      <div className={classes.trackWrap}>
        <div className={classes.track} role="img" aria-label={`${label}: ${lectura}`}>
          {segments.map(
            (s) =>
              s.value > 0 && (
                <span key={s.key} className={classes[s.key]} style={{ flexGrow: s.value }} />
              ),
          )}
          {resto > 0 && <span style={{ flexGrow: resto }} />}
        </div>
        {marcadores.map((m) => (
          <span
            key={m.key}
            className={cx(classes.marker, classes[m.key])}
            style={{ insetInlineStart: `${(m.value / escala) * 100}%` }}
            aria-hidden="true"
          />
        ))}
      </div>
      {!compact && (
        <div
          className={classes.scale}
          style={{ '--tick': `${tick}%` } as CSSProperties}
          aria-hidden="true"
        >
          <span>0</span>
          <span>{formatNumber(escala) + (empaque ? ` ${empaque.unidad}` : u)}</span>
        </div>
      )}
      {legend && !compact && (
        <ul className={classes.legend}>
          {segments.map((s) => (
            <li key={s.key}>
              <span className={cx(classes.swatch, classes[s.key])} aria-hidden="true" />
              {s.text} <b>{formatNumber(s.value)}</b>
              {u}
            </li>
          ))}
          {marcadores.map((m) => (
            <li key={m.key}>
              <span className={cx(classes.swatch, classes[`${m.key}Swatch`])} aria-hidden="true" />
              {m.text} <b>{formatNumber(m.value)}</b>
              {u}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
