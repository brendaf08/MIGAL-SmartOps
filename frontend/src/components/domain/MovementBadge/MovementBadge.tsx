import { Badge, Group, Text } from '@mantine/core';
import { formatNumber } from '@/lib/format';
import { Icon } from '../../ui/Icon/Icon';

export interface MovementBadgeProps {
  tipo: 'Entrada' | 'Salida';
  /** Cantidad positiva; se antepone `+` o `−`. */
  cantidad?: number;
}

/** Tipo de movimiento de Kardex: palabra y flecha, no solo color (RF-KAR-05). */
export function MovementBadge({ tipo, cantidad }: MovementBadgeProps) {
  const entrada = tipo === 'Entrada';

  return (
    <Group gap="xs" wrap="nowrap">
      <Badge
        color={entrada ? 'success' : 'brand'}
        leftSection={<Icon name={entrada ? 'arrow-down' : 'arrow-up'} size={16} />}
      >
        {tipo}
      </Badge>
      {cantidad !== undefined && (
        <Text fw={600} style={{ fontVariantNumeric: 'tabular-nums' }}>
          {entrada ? '+' : '−'}
          {formatNumber(Math.abs(cantidad))}
        </Text>
      )}
    </Group>
  );
}
