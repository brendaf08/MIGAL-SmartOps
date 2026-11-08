import { ActionIcon, Group, Pagination as MantinePagination, Text } from '@mantine/core';
import { formatNumber } from '@/lib/format';
import { Icon } from '../Icon/Icon';

export interface PaginationProps {
  /** Página actual, base 1. */
  page: number;
  total: number;
  pageSize?: number;
  onChange: (page: number) => void;
}

/** Tablet y escritorio: resumen y números de página. Teléfono: flechas y "Página X de N". */
export function Pagination({ page, total, pageSize = 20, onChange }: PaginationProps) {
  const pages = Math.max(1, Math.ceil(total / pageSize));
  const from = total === 0 ? 0 : (page - 1) * pageSize + 1;
  const to = Math.min(page * pageSize, total);

  return (
    <Group justify="space-between" wrap="nowrap">
      <Text c="dimmed" visibleFrom="sm">
        Mostrando {formatNumber(from)}–{formatNumber(to)} de {formatNumber(total)}
      </Text>
      <MantinePagination
        total={pages}
        value={page}
        onChange={onChange}
        withEdges
        visibleFrom="sm"
      />
      <Group hiddenFrom="sm" justify="space-between" wrap="nowrap" w="100%">
        <ActionIcon
          aria-label="Página anterior"
          variant="default"
          disabled={page <= 1}
          onClick={() => onChange(page - 1)}
        >
          <Icon name="chevron-left" />
        </ActionIcon>
        <Text>
          Página {page} de {pages}
        </Text>
        <ActionIcon
          aria-label="Página siguiente"
          variant="default"
          disabled={page >= pages}
          onClick={() => onChange(page + 1)}
        >
          <Icon name="chevron-right" />
        </ActionIcon>
      </Group>
    </Group>
  );
}
