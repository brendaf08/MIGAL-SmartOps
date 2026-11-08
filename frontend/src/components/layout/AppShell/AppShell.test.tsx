import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { AppProviders } from '@/app/AppProviders';
import { NAV } from '@/routes/navigation';
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
    expect(nav).toHaveTextContent('Cotizaciones y ventas');
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
    const items = await screen.findAllByRole('button', { name: 'Despachos', hidden: true });
    await userEvent.click(items[items.length - 1]!);
    expect(onNavigate).toHaveBeenCalledWith('despachos');
  });
});
