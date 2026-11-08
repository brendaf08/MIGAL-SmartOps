import { Modal as MantineModal, useMatches } from '@mantine/core';
import type { ReactNode } from 'react';
import classes from './Modal.module.css';

const SIZES = { sm: 440, md: 560, lg: 760 };

export interface ModalProps {
  open: boolean;
  title: string;
  onClose: () => void;
  size?: keyof typeof SIZES;
  /** Cuerpo del diálogo. */
  children: ReactNode;
  /** Acciones: primero la cancelación y al final la acción principal. */
  footer?: ReactNode;
  /** Impide cerrar mientras se procesa. */
  persistent?: boolean;
}

/** Diálogo centrado en tablet y escritorio; hoja inferior con acciones apiladas en teléfono. */
export function Modal({
  open,
  title,
  onClose,
  size = 'md',
  children,
  footer,
  persistent,
}: ModalProps) {
  const transition = useMatches<'slide-up' | 'pop'>({ base: 'slide-up', sm: 'pop' });

  return (
    <MantineModal
      opened={open}
      onClose={onClose}
      title={title}
      size={SIZES[size]}
      withCloseButton={!persistent}
      closeOnEscape={!persistent}
      transitionProps={{ transition, duration: 200 }}
      classNames={{ inner: classes.inner, content: classes.content }}
    >
      {children}
      {footer && <div className={classes.footer}>{footer}</div>}
    </MantineModal>
  );
}
