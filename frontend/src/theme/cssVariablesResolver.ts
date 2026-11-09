import type { CSSVariablesResolver } from '@mantine/core';

/**
 * Enlaza las variables de color de Mantine con los tokens del sistema de diseño, de modo que los
 * componentes de Mantine adopten las superficies y la tinta de cada tema. Los valores provienen de tokens.css,
 * que cambia según el atributo `data-theme`.
 */
const mapaDeColores = {
  '--mantine-color-body': 'var(--page)',
  '--mantine-color-text': 'var(--ink)',
  '--mantine-color-dimmed': 'var(--ink-muted)',
  '--mantine-color-placeholder': 'var(--ink-subtle)',
  '--mantine-color-default': 'var(--surface)',
  '--mantine-color-default-hover': 'var(--surface-hover)',
  '--mantine-color-default-color': 'var(--ink)',
  '--mantine-color-default-border': 'var(--line-strong)',
  '--mantine-color-error': 'var(--danger)',
  '--mantine-color-anchor': 'var(--brand)',
} as const;

export const cssVariablesResolver: CSSVariablesResolver = () => ({
  variables: {
    '--mantine-font-family': 'var(--font-sans)',
    '--mantine-font-family-headings': 'var(--font-sans)',
    '--mantine-font-family-monospace': 'var(--font-mono)',
    '--mantine-focus-ring-color': 'var(--focus)',
  },
  light: { ...mapaDeColores },
  dark: { ...mapaDeColores },
});
