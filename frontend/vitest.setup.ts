import '@testing-library/jest-dom/vitest';
import { configure } from '@testing-library/react';
import { afterEach, vi } from 'vitest';

/* Con la suite completa en paralelo, las consultas simuladas con latencia superan el segundo por omisión. */
configure({ asyncUtilTimeout: 10000 });

/* jsdom no implementa matchMedia ni ResizeObserver, que Mantine utiliza. */
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    addListener: vi.fn(),
    removeListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }),
});

/* jsdom no implementa scrollIntoView, que usan las listas de opciones de Mantine. */
Element.prototype.scrollIntoView = vi.fn();

class ResizeObserverMock {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
}
vi.stubGlobal('ResizeObserver', ResizeObserverMock);

/* jsdom no implementa document.fonts, que usa el autoajuste de altura de Textarea. */
Object.defineProperty(document, 'fonts', {
  value: { addEventListener: vi.fn(), removeEventListener: vi.fn(), ready: Promise.resolve() },
});

afterEach(() => {
  window.localStorage.clear();
  document.documentElement.removeAttribute('data-theme');
  document.documentElement.removeAttribute('data-mantine-color-scheme');
});
