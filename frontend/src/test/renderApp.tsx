import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { App } from '@/App';
import { AppProviders } from '@/app/AppProviders';
import type { Role } from '@/features/seguridad/roles';

/** Usuarios de prueba por rol, con la forma que guarda SessionProvider. */
const USERS: Record<Role, { name: string; role: Role }> = {
  Administrador: { name: 'Carlos Medina', role: 'Administrador' },
  Vendedor: { name: 'Lucía Torres', role: 'Vendedor' },
  Almacenero: { name: 'Rosa Quispe', role: 'Almacenero' },
  Reponedor: { name: 'Miguel Paredes', role: 'Reponedor' },
};

/** Renderiza la aplicación en una ruta, con o sin sesión iniciada. */
export function renderApp(route: string, role?: Role) {
  if (role) window.sessionStorage.setItem('migal-session', JSON.stringify(USERS[role]));
  return render(
    <AppProviders>
      <MemoryRouter initialEntries={[route]}>
        <App />
      </MemoryRouter>
    </AppProviders>,
  );
}
