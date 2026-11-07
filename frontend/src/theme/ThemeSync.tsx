import { useComputedColorScheme } from '@mantine/core';
import { useEffect } from 'react';

/**
 * Sincroniza el atributo `data-theme` de tokens.css con el esquema de color resuelto por Mantine.
 * No renderiza contenido.
 */
export function ThemeSync() {
  const scheme = useComputedColorScheme('light', { getInitialValueInEffect: false });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', scheme);
  }, [scheme]);

  return null;
}
