import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AppProviders } from '@/app/AppProviders';
import { NAV } from '@/routes/navigation';
import { WAREHOUSE_MODE_STORAGE_KEY, useWarehouseMode } from '@/theme';
import { AppShell } from './AppShell';

const renderShell = (role: string, onNavigate = vi.fn()) => {
  render(
    <AppProviders>
      <AppShell
        nav={NAV}
        role={role}
        current="disponibilidad"
        onNavigate={onNavigate}
        user={{ name: 'Ana Ruiz', role }}
        onLogout={vi.fn()}
        logo="/logo.png"
        appName="MIGAL SmartOps"
      >
        <h1>Contenido</h1>
      </AppShell>
    </AppProviders>,
  );
  return onNavigate;
};

/* jsdom aplica las reglas @media de Mantine sin evaluarlas: las variantes responsive se consultan con `hidden`. */
describe('AppShell', () => {
  it('muestra solo los módulos del rol y marca la página actual', () => {
    renderShell('Vendedor');

    const nav = screen.getAllByRole('navigation', {
      name: 'Navegación principal',
      hidden: true,
    })[0]!;
    expect(nav).toHaveTextContent('Disponibilidad');
    expect(nav).toHaveTextContent('Cotizaciones');
    expect(nav).not.toHaveTextContent('Usuarios y roles');
    expect(
      screen.getAllByRole('button', { name: 'Disponibilidad', hidden: true })[0],
    ).toHaveAttribute('aria-current', 'page');
  });

  it('ofrece enlace para saltar al contenido, cambio de modo y cierre de sesión', () => {
    renderShell('Almacenero');

    expect(screen.getByRole('link', { name: 'Saltar al contenido' })).toHaveAttribute(
      'href',
      '#contenido',
    );
    expect(screen.getByRole('button', { name: 'Cambiar a modo oscuro' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cerrar sesión' })).toBeInTheDocument();
  });

  it('abre el panel de navegación del teléfono con el botón de menú', async () => {
    const onNavigate = renderShell('Almacenero');
    const menu = screen.getByLabelText('Abrir menú');

    expect(menu).toHaveAttribute('aria-expanded', 'false');
    await userEvent.click(menu);
    expect(menu).toHaveAttribute('aria-expanded', 'true');

    // El panel abierto añade una segunda copia de la navegación; se elige la del panel.
    const panel = await screen.findByRole('dialog', { hidden: true });
    await userEvent.click(within(panel).getByRole('button', { name: 'Despachos', hidden: true }));
    expect(onNavigate).toHaveBeenCalledWith('despachos');
    await waitFor(() =>
      expect(screen.getByLabelText('Abrir menú')).toHaveAttribute('aria-expanded', 'false'),
    );
  });

  it('cierra el panel con Escape', async () => {
    renderShell('Almacenero');
    const menu = screen.getByLabelText('Abrir menú');

    await userEvent.click(menu);
    expect(menu).toHaveAttribute('aria-expanded', 'true');
    expect(menu).toHaveAttribute('aria-controls', 'panel-navegacion');

    await userEvent.keyboard('{Escape}');
    await waitFor(() => expect(menu).toHaveAttribute('aria-expanded', 'false'));
    // jsdom no evalúa las reglas @media: el foco real se comprueba en el navegador (390 px).
  });

  it('conserva el título de cada ítem para la barra de íconos de la tablet', () => {
    renderShell('Almacenero');

    expect(screen.getAllByRole('button', { name: 'Despachos', hidden: true })[0]).toHaveAttribute(
      'title',
      'Despachos',
    );
  });

  it('ofrece el interruptor del modo almacén solo cuando se le pasa la acción', () => {
    renderShell('Almacenero');
    expect(screen.queryByRole('button', { name: 'Modo almacén', hidden: true })).toBeNull();
  });
});

const ROL = 'Almacenero';

/** Estructura conectada al hook del modo almacén, como la usa `ShellLayout`. */
function ShellLayoutPrueba() {
  const { warehouse, toggle } = useWarehouseMode();
  return (
    <AppShell
      nav={NAV}
      role={ROL}
      current="disponibilidad"
      onNavigate={vi.fn()}
      user={{ name: 'Ana Ruiz', role: ROL }}
      onLogout={vi.fn()}
      logo="/logo.png"
      appName="MIGAL SmartOps"
      warehouse={warehouse}
      onToggleWarehouse={toggle}
    >
      <h1>Contenido</h1>
    </AppShell>
  );
}

describe('Modo almacén', () => {
  afterEach(() => {
    window.localStorage.clear();
    delete document.documentElement.dataset.warehouse;
  });

  it('se activa con el interruptor, queda marcado como presionado y se conserva en el equipo', async () => {
    render(
      <AppProviders>
        <ShellLayoutPrueba />
      </AppProviders>,
    );
    const boton = screen.getByRole('button', { name: 'Modo almacén', hidden: true });
    expect(boton).toHaveAttribute('aria-pressed', 'false');

    await userEvent.click(boton);

    expect(boton).toHaveAttribute('aria-pressed', 'true');
    expect(document.documentElement.dataset.warehouse).toBe('true');
    expect(window.localStorage.getItem(WAREHOUSE_MODE_STORAGE_KEY)).toBe('true');

    await userEvent.click(boton);
    expect(document.documentElement.dataset.warehouse).toBeUndefined();
    expect(window.localStorage.getItem(WAREHOUSE_MODE_STORAGE_KEY)).toBe('false');
  });

  it('recupera el modo guardado al abrir la aplicación', async () => {
    window.localStorage.setItem(WAREHOUSE_MODE_STORAGE_KEY, 'true');

    render(
      <AppProviders>
        <ShellLayoutPrueba />
      </AppProviders>,
    );

    expect(screen.getByRole('button', { name: 'Modo almacén', hidden: true })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    expect(document.documentElement.dataset.warehouse).toBe('true');
  });
});
