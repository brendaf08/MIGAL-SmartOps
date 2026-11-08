import type { ReactElement } from 'react';
import { Navigate, Route, Routes } from 'react-router';
import { EmptyState } from '@/components/ui/EmptyState/EmptyState';
import { DisponibilidadPage } from '@/features/inventario/DisponibilidadPage';
import { RequireAuth, RequireRole } from '@/features/seguridad/guards';
import { LoginPage } from '@/features/seguridad/LoginPage';
import { useSession } from '@/features/seguridad/session';
import { NAV } from './navigation';
import { PendientePage } from './PendientePage';
import { ShellLayout } from './ShellLayout';

const ITEMS = NAV.flatMap((group) => group.items);

/** Pantalla de cada módulo por clave de navegación. Los módulos sin pantalla muestran un aviso. */
const PAGES: Record<string, ReactElement> = {
  disponibilidad: <DisponibilidadPage />,
};

/** Lleva al primer módulo visible para el rol del usuario. */
function Home() {
  const { user } = useSession();
  const first = ITEMS.find((item) => !item.roles || (user && item.roles.includes(user.role)));
  return <Navigate to={`/${first?.key ?? 'agente-ia'}`} replace />;
}

/** Rutas públicas y protegidas por rol; la navegación y las rutas salen de la misma definición (`NAV`). */
export function AppRoutes() {
  return (
    <Routes>
      <Route path="/acceso" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<ShellLayout />}>
          <Route index element={<Home />} />
          {ITEMS.map((item) => (
            <Route key={item.key} element={<RequireRole roles={item.roles} />}>
              <Route
                path={item.key}
                element={PAGES[item.key] ?? <PendientePage title={item.label} />}
              />
            </Route>
          ))}
          <Route
            path="*"
            element={
              <EmptyState
                title="Página no encontrada"
                description="La dirección no corresponde a ningún módulo."
              />
            }
          />
        </Route>
      </Route>
    </Routes>
  );
}
