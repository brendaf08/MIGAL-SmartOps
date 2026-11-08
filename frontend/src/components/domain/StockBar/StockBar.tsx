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
}

/** Barra segmentada del reparto del stock: libre, reservado y pendiente de abastecimiento (con rayas). */
export function StockBar({
  libre,
  reservado,
  pendiente = 0,
  unidad,
  label = 'Distribución de stock',
  legend = true,
}: StockBarProps) {
  const segments = [
    { key: 'libre', value: libre, text: 'Libre' },
    { key: 'reservado', value: reservado, text: 'Reservado' },
    { key: 'pendiente', value: pendiente, text: 'Pendiente de abastecimiento' },
  ].filter((s) => s.value > 0 || s.key !== 'pendiente');
  const total = libre + reservado + pendiente;
  const u = unidad ? ` ${unidad}` : '';

  return (
    <div className={classes.root}>
      <div
        className={classes.track}
        role="img"
        aria-label={`${label}: ${segments.map((s) => `${s.text} ${formatNumber(s.value)}`).join(', ')}`}
      >
        {segments.map(
          (s) =>
            s.value > 0 && (
              <span key={s.key} className={classes[s.key]} style={{ flexGrow: s.value }} />
            ),
        )}
      </div>
      <div className={classes.scale} aria-hidden="true">
        <span>0</span>
        <span>{formatNumber(total) + u}</span>
      </div>
      {legend && (
        <ul className={classes.legend}>
          {segments.map((s) => (
            <li key={s.key}>
              <span className={`${classes.swatch} ${classes[s.key]}`} aria-hidden="true" />
              {s.text} <b>{formatNumber(s.value)}</b>
              {u}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
