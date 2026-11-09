import { Button, FileButton, Stack, Text } from '@mantine/core';
import { Dropzone as MantineDropzone, MIME_TYPES, type FileRejection } from '@mantine/dropzone';
import { Icon } from '../Icon/Icon';
import classes from './Dropzone.module.css';

const MAX_SIZE = 10 * 1024 ** 2;

export interface DropzoneProps {
  /** Tipos admitidos; por omisión JPG, PNG y PDF. */
  accept?: string[];
  /** Tamaño máximo en bytes (10 MB por omisión). */
  maxSize?: number;
  multiple?: boolean;
  /** Agrega "Tomar foto", que abre la cámara trasera del teléfono o la tablet. */
  camera?: boolean;
  disabled?: boolean;
  onDrop: (files: File[]) => void;
  onReject?: (rejections: FileRejection[]) => void;
}

/** Zona de carga de documentos para OCR: arrastre y selección, con cámara como modo principal en teléfono. */
export function Dropzone({
  accept = [MIME_TYPES.jpeg, MIME_TYPES.png, MIME_TYPES.pdf],
  maxSize = MAX_SIZE,
  multiple,
  camera,
  disabled,
  onDrop,
  onReject,
}: DropzoneProps) {
  return (
    <Stack gap="sm">
      {camera && (
        <FileButton
          onChange={(file) => file && onDrop([file])}
          accept="image/*"
          capture="environment"
          disabled={disabled}
        >
          {(props) => (
            <Button {...props} size="lg" leftSection={<Icon name="camera" size={24} />}>
              Tomar foto
            </Button>
          )}
        </FileButton>
      )}
      <MantineDropzone
        className={classes.root}
        accept={accept}
        maxSize={maxSize}
        multiple={multiple}
        disabled={disabled}
        onDrop={onDrop}
        onReject={onReject}
      >
        <Stack align="center" gap="xs" ta="center">
          <Icon name="upload" size={24} />
          <Text fw={600}>Arrastrar documentos o seleccionar archivos</Text>
          <Text c="dimmed">JPG, PNG o PDF de hasta {maxSize / 1024 ** 2} MB</Text>
        </Stack>
      </MantineDropzone>
    </Stack>
  );
}
