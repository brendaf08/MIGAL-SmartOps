import { MantineProvider } from '@mantine/core';
import { ModalsProvider } from '@mantine/modals';
import { Notifications } from '@mantine/notifications';
import { QueryClientProvider } from '@tanstack/react-query';
import { useState, type ReactNode } from 'react';
import { SessionProvider } from '@/features/seguridad/SessionProvider';
import { colorSchemeManager, cssVariablesResolver, theme, ThemeSync } from '@/theme';
import { createQueryClient } from './queryClient';

interface AppProvidersProps {
  children: ReactNode;
}

/** Proveedores globales de la aplicación: tema, notificaciones, diálogos y consultas al servidor. */
export function AppProviders({ children }: AppProvidersProps) {
  const [queryClient] = useState(createQueryClient);

  return (
    <MantineProvider
      theme={theme}
      cssVariablesResolver={cssVariablesResolver}
      colorSchemeManager={colorSchemeManager}
      defaultColorScheme="auto"
    >
      <ThemeSync />
      <Notifications position="top-right" />
      <ModalsProvider>
        <QueryClientProvider client={queryClient}>
          <SessionProvider>{children}</SessionProvider>
        </QueryClientProvider>
      </ModalsProvider>
    </MantineProvider>
  );
}
