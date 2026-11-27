/*
 * Aplica el tema guardado antes del primer pintado, para evitar un destello. Es un archivo
 * propio y no un script en línea porque la política de seguridad de contenido (CSP) solo admite scripts del mismo
 * origen.
 */
(function () {
  try {
    var stored = localStorage.getItem('migal-color-scheme');
    var dark =
      stored === 'dark' ||
      ((stored === null || stored === 'auto') &&
        window.matchMedia('(prefers-color-scheme: dark)').matches);
    var scheme = dark ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', scheme);
    document.documentElement.setAttribute('data-mantine-color-scheme', scheme);
  } catch (error) {
    /* Sin acceso a localStorage: se mantiene el modo claro. */
  }
})();
