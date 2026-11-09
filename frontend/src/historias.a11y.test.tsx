import { composeStories } from '@storybook/react-vite';
import { render } from '@testing-library/react';
import type { ComponentType } from 'react';
import { MemoryRouter } from 'react-router';
import { describe, expect, it } from 'vitest';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { AppProviders } from '@/app/AppProviders';

expect.extend(matchers);

type Modulo = Parameters<typeof composeStories>[0];

/** Todas las historias del catálogo, agrupadas por archivo. */
const modulos = import.meta.glob<Modulo>('./components/**/*.stories.tsx', { eager: true });

const casos = Object.entries(modulos).flatMap(([ruta, modulo]) =>
  Object.entries(composeStories(modulo)).map(([nombre, Historia]) => ({
    nombre: `${ruta.replace('./components/', '').replace('.stories.tsx', '')} > ${nombre}`,
    Historia: Historia as ComponentType,
  })),
);

describe('Catálogo de componentes (Storybook)', () => {
  it('incluye historias de los componentes de interfaz, de dominio y de estructura', () => {
    expect(casos.length).toBeGreaterThanOrEqual(60);
  });

  it.each(casos)('$nombre no tiene violaciones de accesibilidad', async ({ Historia }) => {
    const { container } = render(
      <AppProviders>
        <MemoryRouter>
          <Historia />
        </MemoryRouter>
      </AppProviders>,
    );

    // El contraste se verifica con los tokens, no en jsdom, y la regla de regiones aplica a páginas, no a componentes aislados.
    expect(
      await axe(document.body, {
        rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
      }),
    ).toHaveNoViolations();
    container.remove();
  });
});
