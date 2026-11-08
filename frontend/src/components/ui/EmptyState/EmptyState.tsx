import { Stack, Text, Title } from '@mantine/core';
import type { ReactNode } from 'react';
import { Icon, type IconProps } from '../Icon/Icon';
import classes from './EmptyState.module.css';

export interface EmptyStateProps {
  title: string;
  description?: string;
  icon?: IconProps['name'];
  /** Acción que permite recuperarse (por ejemplo, limpiar filtros). */
  action?: ReactNode;
}

/** Estado sin resultados o sin datos, con una salida clara para el usuario. */
export function EmptyState({ title, description, icon = 'inbox', action }: EmptyStateProps) {
  return (
    <Stack align="center" gap="xs" p="xl" ta="center">
      <span className={classes.icon}>
        <Icon name={icon} size={24} />
      </span>
      <Title order={3}>{title}</Title>
      {description && <Text c="dimmed">{description}</Text>}
      {action}
    </Stack>
  );
}
