import '@mantine/core/styles.css';
import '@/theme/tokens.css';
import '@/theme/global.css';
import '@/theme/fonts';

import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from '@/App';
import { AppProviders } from '@/app/AppProviders';

const container = document.getElementById('root');
if (!container) {
  throw new Error('No se encontró el elemento raíz de la aplicación.');
}

createRoot(container).render(
  <StrictMode>
    <AppProviders>
      <App />
    </AppProviders>
  </StrictMode>,
);
