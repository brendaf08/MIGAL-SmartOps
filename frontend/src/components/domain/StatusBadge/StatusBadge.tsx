import { Badge } from '@mantine/core';
import { cx } from '@/lib/cx';
import { Icon } from '../../ui/Icon/Icon';
import { STATUS, type Definition, type StatusEntity } from './status';
import classes from './StatusBadge.module.css';

export interface StatusBadgeProps {
  entity: StatusEntity;
  /** Valor exacto de la enumeración del backend. */
  value: string;
  /** Texto alternativo; por omisión, el del mapa central. */
  label?: string;
  /** `sello`: doble contorno para el encabezado de un documento; una sola vez por página. */
  variant?: 'badge' | 'sello';
}

/** Insignia de estado: tono, ícono de señalización y texto, igual en todas las pantallas. */
export function StatusBadge({ entity, value, label, variant = 'badge' }: StatusBadgeProps) {
  const [tone, text, icon] = (STATUS[entity] as Record<string, Definition>)[value] ?? [
    'neutral',
    value,
    'info',
  ];

  return (
    <Badge
      className={cx(classes.root, variant === 'sello' && classes.sello)}
      data-tone={tone}
      leftSection={variant === 'sello' ? undefined : <Icon name={icon} size={16} />}
    >
      {label ?? text}
    </Badge>
  );
}
