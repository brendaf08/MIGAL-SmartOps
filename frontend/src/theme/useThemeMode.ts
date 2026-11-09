import { useComputedColorScheme, useMantineColorScheme } from '@mantine/core';

/** Acceso al modo actual y al cambio entre claro y oscuro, para el botón de la cabecera. */
export function useThemeMode() {
  const { setColorScheme } = useMantineColorScheme();
  const scheme = useComputedColorScheme('light', { getInitialValueInEffect: false });

  return {
    scheme,
    toggle: () => setColorScheme(scheme === 'dark' ? 'light' : 'dark'),
  };
}
