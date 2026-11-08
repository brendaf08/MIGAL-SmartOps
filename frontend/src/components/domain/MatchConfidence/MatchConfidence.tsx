import { Badge, Group, Progress, Text } from '@mantine/core';
import { Icon } from '../../ui/Icon/Icon';

export interface MatchConfidenceProps {
  /** Similitud de 0 a 1. */
  score: number;
  /** Umbrales de ejemplo, pendientes de definir en el servicio Python. */
  thresholds?: { high: number; low: number };
}

const LEVELS = {
  high: { color: 'success', icon: 'status-ok', text: 'Coincidencia alta' },
  mid: { color: 'warning', icon: 'alert-triangle', text: 'Revisar coincidencia' },
  low: { color: 'danger', icon: 'status-danger', text: 'Sin coincidencia' },
} as const;

/** Sugiere, no decide: aun con coincidencia alta el usuario confirma la asociación (RF-FUZ-06). */
export function MatchConfidence({
  score,
  thresholds = { high: 0.85, low: 0.6 },
}: MatchConfidenceProps) {
  const level = score >= thresholds.high ? 'high' : score >= thresholds.low ? 'mid' : 'low';
  const { color, icon, text } = LEVELS[level];
  const percent = Math.round(score * 100);

  return (
    <div>
      <Group justify="space-between" wrap="nowrap" mb={4}>
        <Badge color={color} leftSection={<Icon name={icon} size={16} />}>
          {text}
        </Badge>
        <Text fw={600} ff="var(--font-display)">
          {percent} %
        </Text>
      </Group>
      <Progress
        value={percent}
        color={color}
        size="sm"
        role="meter"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={percent}
        aria-label="Similitud con el catálogo"
      />
      {level === 'low' && (
        <Text size="sm" c="dimmed" mt={4}>
          Se requiere validación manual antes de continuar.
        </Text>
      )}
    </div>
  );
}
