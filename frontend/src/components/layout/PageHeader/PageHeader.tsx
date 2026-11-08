import { Anchor, Breadcrumbs, Group, Text, Title } from '@mantine/core';
import type { ReactNode } from 'react';
import classes from './PageHeader.module.css';

export interface PageHeaderProps {
  title: string;
  description?: string;
  /** Ruta de navegación; el último elemento es la página actual. */
  breadcrumbs?: { label: string; href?: string }[];
  /** Sello de estado de la entidad (`StatusBadge`). */
  status?: ReactNode;
  /** Acciones de menor a mayor énfasis; la principal queda a la derecha. */
  actions?: ReactNode;
}

/** Cabecera estándar de cada página: una sola por página, con el único `h1`. */
export function PageHeader({ title, description, breadcrumbs, status, actions }: PageHeaderProps) {
  return (
    <header className={classes.root}>
      {breadcrumbs && (
        <Breadcrumbs aria-label="Ruta de navegación">
          {breadcrumbs.map(({ label, href }, i) =>
            href && i < breadcrumbs.length - 1 ? (
              <Anchor key={label} href={href}>
                {label}
              </Anchor>
            ) : (
              <Text key={label} aria-current="page">
                {label}
              </Text>
            ),
          )}
        </Breadcrumbs>
      )}
      <div className={classes.row}>
        <div>
          <Group gap="sm">
            <Title order={1} className={classes.title}>
              {title}
            </Title>
            {status}
          </Group>
          {description && <Text c="dimmed">{description}</Text>}
        </div>
        {actions && <Group className={classes.actions}>{actions}</Group>}
      </div>
    </header>
  );
}
