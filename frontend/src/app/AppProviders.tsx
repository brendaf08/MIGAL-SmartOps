import { MantineProvider } from '@mantine/core';
import type { ReactNode } from 'react';
import { colorSchemeManager, cssVariablesResolver, theme, ThemeSync } from '@/theme';

interface AppProvidersProps {
  children: ReactNode;
}

/** Proveedores globales de la aplicación. */
export function AppProviders({ children }: AppProvidersProps) {
  return (
    <MantineProvider
      theme={theme}
      cssVariablesResolver={cssVariablesResolver}
      colorSchemeManager={colorSchemeManager}
      defaultColorScheme="auto"
    >
      <ThemeSync />
      {children}
    </MantineProvider>
  );
}
