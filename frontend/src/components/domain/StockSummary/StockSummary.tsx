import { Text } from '@mantine/core';
import { formatNumber } from '@/lib/format';
import { cx } from '@/lib/cx';
import { StatusBadge } from '../StatusBadge/StatusBadge';
import { stockLevel } from '../StatusBadge/status';
import classes from './StockSummary.module.css';

export interface StockSummaryProps {
  fisico: number;
  reservado?: number;
  /** Por omisión, físico menos reservado. */
  libre?: number;
  minimo?: number;
  unidad?: string;
  /** Variante para filas de tabla. */
  compact?: boolean;
  hideLevel?: boolean;
}

/** Existencia de un producto: la cantidad libre es el foco; físico y reservado quedan como contexto. */
export function StockSummary({
  fisico,
  reservado = 0,
  libre = fisico - reservado,
  minimo,
  unidad,
  compact,
  hideLevel,
}: StockSummaryProps) {
  return (
    <div className={cx(classes.root, compact && classes.compact)}>
      <dl className={classes.grid}>
        <div className={classes.free}>
          <dt>Libre</dt>
          <dd>
            {formatNumber(libre)}
            {unidad && <span className={classes.unit}> {unidad}</span>}
          </dd>
        </div>
        <div>
          <dt>Físico</dt>
          <dd>{formatNumber(fisico)}</dd>
        </div>
        <div>
          <dt>Reservado</dt>
          <dd>{formatNumber(reservado)}</dd>
        </div>
      </dl>
      {minimo !== undefined && !hideLevel && (
        <div className={classes.level}>
          <StatusBadge entity="stock" value={stockLevel(libre, minimo)} />
          <Text size="xs" c="dimmed">
            Mínimo: {formatNumber(minimo)}
          </Text>
        </div>
      )}
    </div>
  );
}
