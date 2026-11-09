import { localStorageColorSchemeManager } from '@mantine/core';

/** Clave de localStorage donde se conserva la elección del usuario ("light", "dark" o "auto"). */
export const COLOR_SCHEME_STORAGE_KEY = 'migal-color-scheme';

export const colorSchemeManager = localStorageColorSchemeManager({
  key: COLOR_SCHEME_STORAGE_KEY,
});
