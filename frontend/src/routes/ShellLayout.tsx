import { Outlet, useLocation, useNavigate } from 'react-router';
import logo from '@/assets/progresol-logo.png';
import { AppShell } from '@/components/layout/AppShell/AppShell';
import { useSession } from '@/features/seguridad/session';
import { NAV } from './navigation';

/** Marco de las rutas internas: la clave de cada ítem de navegación es su ruta. */
export function ShellLayout() {
  const { user, logout } = useSession();
  const navigate = useNavigate();
  const current = useLocation().pathname.split('/')[1];

  if (!user) return null;

  return (
    <AppShell
      nav={NAV}
      role={user.role}
      current={current}
      onNavigate={(key) => void navigate(`/${key}`)}
      user={user}
      onLogout={logout}
      logo={logo}
      appName="MIGAL SmartOps"
      subtitle="Ferretería Progresol Migal"
    >
      <Outlet />
    </AppShell>
  );
}
