import { Alert as MantineAlert } from '@mantine/core';
import type { ReactNode } from 'react';
import { Icon } from '../Icon/Icon';

const TONES = {
  info: 'info',
  success: 'status-ok',
  warning: 'alert-triangle',
  danger: 'status-danger',
} as const;

export interface AlertProps {
  tone?: keyof typeof TONES;
  title: string;
  children?: ReactNode;
}

/** Aviso en línea: el tono se acompaña de la forma de señalización y de un título, no solo de color. */
export function Alert({ tone = 'info', title, children }: AlertProps) {
  return (
    <MantineAlert
      color={tone}
      title={title}
      icon={<Icon name={TONES[tone]} size={24} />}
      role={tone === 'danger' ? 'alert' : 'status'}
    >
      {children}
    </MantineAlert>
  );
}
