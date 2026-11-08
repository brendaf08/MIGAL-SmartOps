import type { StorybookConfig } from '@storybook/react-vite';

/** Catálogo de componentes: una historia por componente, con verificación de accesibilidad (axe). */
const config: StorybookConfig = {
  framework: '@storybook/react-vite',
  stories: ['../src/**/*.stories.@(ts|tsx)'],
  core: { disableTelemetry: true },
  addons: ['@storybook/addon-a11y', '@storybook/addon-docs'],
};

export default config;
