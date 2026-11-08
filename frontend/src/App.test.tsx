import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { COLOR_SCHEME_STORAGE_KEY } from '@/theme';
import { renderApp } from '@/test/renderApp';

afterEach(() => window.sessionStorage.clear());

describe('Tema de la aplicación', () => {
  it('aplica el modo claro como valor inicial y lo expone en data-theme', async () => {
    renderApp('/disponibilidad', 'Almacenero');

    expect(
      await screen.findByRole('button', { name: 'Cambiar a modo oscuro' }),
    ).toBeInTheDocument();
    expect(document.documentElement).toHaveAttribute('data-theme', 'light');
  });

  it('cambia a modo oscuro, actualiza data-theme y conserva la elección', async () => {
    renderApp('/disponibilidad', 'Almacenero');

    await userEvent.click(await screen.findByRole('button', { name: 'Cambiar a modo oscuro' }));

    expect(document.documentElement).toHaveAttribute('data-theme', 'dark');
    expect(window.localStorage.getItem(COLOR_SCHEME_STORAGE_KEY)).toBe('dark');
    expect(screen.getByRole('button', { name: 'Cambiar a modo claro' })).toBeInTheDocument();
  });

  it('recupera el modo oscuro guardado por el usuario', async () => {
    window.localStorage.setItem(COLOR_SCHEME_STORAGE_KEY, 'dark');
    renderApp('/disponibilidad', 'Almacenero');

    await screen.findByRole('button', { name: 'Cambiar a modo claro' });
    expect(document.documentElement).toHaveAttribute('data-theme', 'dark');
  });
});

describe('Rutas y roles', () => {
  it('lleva a iniciar sesión cuando no hay sesión', async () => {
    renderApp('/disponibilidad');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'MIGAL SmartOps' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Iniciar sesión' })).toBeInTheDocument();
  });

  it('redirige al inicio cuando el rol no tiene acceso al módulo', async () => {
    renderApp('/usuarios', 'Vendedor');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Disponibilidad' }),
    ).toBeInTheDocument();
  });

  it('abre el primer módulo del rol desde la raíz', async () => {
    renderApp('/', 'Reponedor');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Alertas de stock' }),
    ).toBeInTheDocument();
  });
});
