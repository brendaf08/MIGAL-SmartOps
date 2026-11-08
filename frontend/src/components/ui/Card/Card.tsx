import { Group, Paper, Title } from '@mantine/core';
import type { ReactNode } from 'react';
import classes from './Card.module.css';

export interface CardProps {
  title?: string;
  /** Nivel del encabezado; 3 por omisión, ajustar para no saltar niveles. */
  level?: 2 | 3 | 4;
  description?: string;
  /** Una o dos acciones del encabezado. */
  actions?: ReactNode;
  /** Acciones del flujo (confirmar, cancelar). */
  footer?: ReactNode;
  /** `false` para contenido a ancho completo, como tablas. */
  padded?: boolean;
  children: ReactNode;
}

/** Contenedor de una sola idea, con encabezado opcional, cuerpo y pie. */
export function Card({
  title,
  level = 3,
  description,
  actions,
  footer,
  padded = true,
  children,
}: CardProps) {
  return (
    <Paper component="section" className={classes.root} aria-label={title}>
      {(title || actions) && (
        <Group className={classes.head} justify="space-between" wrap="nowrap">
          <div>
            {title && (
              <Title order={level} size="h4">
                {title}
              </Title>
            )}
            {description && <p className={classes.description}>{description}</p>}
          </div>
          {actions && <Group gap="xs">{actions}</Group>}
        </Group>
      )}
      <div className={padded ? classes.body : undefined}>{children}</div>
      {footer && (
        <Group className={classes.footer} justify="flex-end">
          {footer}
        </Group>
      )}
    </Paper>
  );
}
