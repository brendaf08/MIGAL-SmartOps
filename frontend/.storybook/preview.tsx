import '@mantine/core/styles.css';
import '@mantine/notifications/styles.css';
import '@mantine/dropzone/styles.css';
import '@/theme/tokens.css';
import '@/theme/global.css';
import '@/theme/fonts';

import type { Preview } from '@storybook/react-vite';
import { MemoryRouter } from 'react-router';
import { AppProviders } from '@/app/AppProviders';
import { Tema } from './Tema';

const preview: Preview = {
  globalTypes: {
    tema: {
      description: 'Tema de color',
      toolbar: {
        title: 'Tema',
        icon: 'circlehollow',
        items: [
          { value: 'light', title: 'Claro' },
          { value: 'dark', title: 'Oscuro tenue' },
        ],
        dynamicTitle: true,
      },
    },
  },
  initialGlobals: { tema: 'light' },
  decorators: [
    (Story, { globals }) => (
      <AppProviders>
        <MemoryRouter>
          <Tema tema={globals['tema'] === 'dark' ? 'dark' : 'light'}>
            <Story />
          </Tema>
        </MemoryRouter>
      </AppProviders>
    ),
  ],
  parameters: {
    layout: 'padded',
    a11y: { test: 'error', config: { rules: [{ id: 'region', enabled: false }] } },
  },
};

export default preview;
