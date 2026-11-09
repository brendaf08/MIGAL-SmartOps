import { ActionIcon, Group, Loader, Text } from '@mantine/core';
import { Icon } from '../Icon/Icon';

export interface FileItemProps {
  name: string;
  /** Tamaño en bytes. */
  size: number;
  status: 'processing' | 'processed' | 'error';
  onRemove?: () => void;
}

const formatSize = (bytes: number) =>
  bytes < 1024 ** 2 ? `${Math.round(bytes / 1024)} KB` : `${(bytes / 1024 ** 2).toFixed(1)} MB`;

const STATUS = {
  processing: 'Procesando',
  processed: 'Procesado por OCR',
  error: 'No se pudo leer',
} as const;

/** Archivo cargado con su estado de procesamiento. */
export function FileItem({ name, size, status, onRemove }: FileItemProps) {
  return (
    <Group wrap="nowrap" gap="sm">
      {status === 'processing' ? (
        <Loader size={20} />
      ) : (
        <Icon name={status === 'error' ? 'status-danger' : 'image'} />
      )}
      <div style={{ flex: 1, minWidth: 0 }}>
        <Text fw={600} truncate>
          {name}
        </Text>
        <Text size="sm" c="dimmed">
          {formatSize(size)} · {STATUS[status]}
        </Text>
      </div>
      {onRemove && (
        <ActionIcon aria-label={`Quitar ${name}`} onClick={onRemove}>
          <Icon name="close" />
        </ActionIcon>
      )}
    </Group>
  );
}
