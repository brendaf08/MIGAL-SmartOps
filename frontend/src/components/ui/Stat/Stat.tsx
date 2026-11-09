import { Text } from '@mantine/core';
import { Icon } from '../Icon/Icon';
import classes from './Stat.module.css';

const FLAG = { warning: 'alert-triangle', danger: 'status-danger', success: 'status-ok' } as const;

export interface StatProps {
  label: string;
  /** Texto ya formateado; sin datos se muestra un guion largo, no cero. */
  value?: string;
  hint?: string;
  /** Se expresa con el marcador de señalización junto a la etiqueta. */
  tone?: keyof typeof FLAG;
}

/** Indicador con una cifra destacada para alertas operativas. */
export function Stat({ label, value = '—', hint, tone }: StatProps) {
  return (
    <div className={classes.root} data-tone={tone}>
      <Text className={classes.label}>
        {tone && <Icon name={FLAG[tone]} size={16} className={classes.flag} />}
        {label}
      </Text>
      <Text className={classes.value}>{value}</Text>
      {hint && (
        <Text size="sm" c="dimmed">
          {hint}
        </Text>
      )}
    </div>
  );
}
