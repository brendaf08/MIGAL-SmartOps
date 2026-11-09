import { useMantineColorScheme } from '@mantine/core';
import { useEffect, type ReactNode } from 'react';

/** Aplica el tema elegido en la barra de herramientas de Storybook. */
export function Tema({ tema, children }: { tema: 'light' | 'dark'; children: ReactNode }) {
  const { setColorScheme } = useMantineColorScheme();
  useEffect(() => setColorScheme(tema), [tema, setColorScheme]);
  return children;
}
