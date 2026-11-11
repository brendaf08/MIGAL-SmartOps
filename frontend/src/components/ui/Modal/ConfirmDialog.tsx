import { Button, List, Text } from '@mantine/core';
import type { ReactNode } from 'react';
import { Modal } from './Modal';

export interface ConfirmDialogProps {
  open: boolean;
  /** Nombra la acción y el objeto: "Cancelar la venta V-0042". */
  title: string;
  description?: string;
  /** Efectos sobre inventario, reservas y Kardex. */
  consequences?: string[];
  tone?: 'danger' | 'primary';
  /** Repite el verbo de la acción; nunca "Aceptar" ni "Sí". */
  confirmLabel: string;
  cancelLabel?: string;
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
  /** Contenido adicional, por ejemplo un campo de captura, que se muestra tras las consecuencias. */
  children?: ReactNode;
}

/** Confirmación de acciones irreversibles. El foco inicial recae en cancelar, la acción segura. */
export function ConfirmDialog({
  open,
  title,
  description,
  consequences,
  tone = 'primary',
  confirmLabel,
  cancelLabel = 'Cancelar',
  loading,
  onConfirm,
  onCancel,
  children,
}: ConfirmDialogProps) {
  return (
    <Modal
      open={open}
      title={title}
      size="sm"
      onClose={onCancel}
      persistent={loading}
      footer={
        <>
          <Button variant="default" data-autofocus disabled={loading} onClick={onCancel}>
            {cancelLabel}
          </Button>
          <Button
            color={tone === 'danger' ? 'danger' : undefined}
            loading={loading}
            onClick={onConfirm}
          >
            {confirmLabel}
          </Button>
        </>
      }
    >
      {description && <Text>{description}</Text>}
      {consequences && (
        <List mt="sm" spacing="xs">
          {consequences.map((c) => (
            <List.Item key={c}>{c}</List.Item>
          ))}
        </List>
      )}
      {children}
    </Modal>
  );
}
