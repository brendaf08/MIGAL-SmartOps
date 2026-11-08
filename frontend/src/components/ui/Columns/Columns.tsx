import { Box, type BoxProps, type ElementProps } from '@mantine/core';
import type { CSSProperties } from 'react';
import classes from './Columns.module.css';

export interface ColumnsProps extends BoxProps, ElementProps<'div'> {
  /** Disposición en escritorio; en tablet y teléfono se apila según Columns.module.css. */
  layout?: 'two' | 'three' | 'main-aside' | 'aside-main';
  /** Separación `space-N` de la escala de espaciado. */
  gap?: 1 | 2 | 3 | 4 | 5 | 6 | 8;
}

/** Columnas que se apilan al reducirse el ancho. El orden del código es el orden de lectura en teléfono. */
export function Columns({ layout = 'two', gap = 5, style, ...props }: ColumnsProps) {
  return (
    <Box
      className={classes.root}
      mod={{ layout }}
      style={[{ '--gap': `var(--space-${gap})` } as CSSProperties, style]}
      {...props}
    />
  );
}
