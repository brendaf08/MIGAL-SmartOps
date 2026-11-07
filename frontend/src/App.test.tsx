import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { App } from '@/App';
import { AppProviders } from '@/app/AppProviders';
import { COLOR_SCHEME_STORAGE_KEY } from '@/theme';

function renderApp() {
  return render(
    <AppProviders>
      <App />
    </AppProviders>,
  );
}

describe('Tema de la aplicación', () => {
  it('aplica el modo claro como valor inicial y lo expone en data-theme', () => {
    renderApp();

    expect(document.documentElement).toHaveAttribute('data-theme', 'light');
    expect(screen.getByRole('button', { name: 'Cambiar a modo oscuro' })).toBeInTheDocument();
  });

  it('cambia a modo oscuro, actualiza data-theme y conserva la elección', async () => {
    const user = userEvent.setup();
    renderApp();

    await user.click(screen.getByRole('button', { name: 'Cambiar a modo oscuro' }));

    expect(document.documentElement).toHaveAttribute('data-theme', 'dark');
    expect(window.localStorage.getItem(COLOR_SCHEME_STORAGE_KEY)).toBe('dark');
    expect(screen.getByRole('button', { name: 'Cambiar a modo claro' })).toBeInTheDocument();
  });

  it('recupera el modo oscuro guardado por el usuario', () => {
    window.localStorage.setItem(COLOR_SCHEME_STORAGE_KEY, 'dark');
    renderApp();

    expect(document.documentElement).toHaveAttribute('data-theme', 'dark');
  });
});
