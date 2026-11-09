import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';

afterEach(() => window.sessionStorage.clear());

describe('LoginPage', () => {
  it('exige usuario y contraseña con mensajes que indican cómo corregir', async () => {
    renderApp('/acceso');

    await userEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    expect(await screen.findByText('Ingresar el usuario.')).toBeInTheDocument();
    expect(screen.getByText('Ingresar la contraseña.')).toBeInTheDocument();
  });

  it('informa credenciales incorrectas sin revelar cuál falló', async () => {
    renderApp('/acceso');

    await userEvent.type(screen.getByLabelText(/Usuario/), 'rquispe');
    await userEvent.type(screen.getByLabelText(/Contraseña/), 'incorrecta');
    await userEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Credenciales incorrectas');
  });

  it('inicia sesión y abre el módulo del rol', async () => {
    renderApp('/acceso');

    await userEvent.type(screen.getByLabelText(/Usuario/), 'rquispe');
    await userEvent.type(screen.getByLabelText(/Contraseña/), 'demo1234');
    await userEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Disponibilidad' }),
    ).toBeInTheDocument();
  });
});
