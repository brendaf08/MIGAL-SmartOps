import { Navigate, Outlet, useLocation } from 'react-router';
import { useSession } from './session';

/** Rutas internas: exigen sesión y conservan el destino para volver tras iniciar sesión. */
export function RequireAuth() {
  const { user } = useSession();
  const location = useLocation();
  return user ? <Outlet /> : <Navigate to="/acceso" replace state={{ from: location.pathname }} />;
}

/** Una ruta oculta en la navegación equivale a una ruta no accesible. */
export function RequireRole({ roles }: { roles?: readonly string[] }) {
  const { user } = useSession();
  return !roles || (user && roles.includes(user.role)) ? <Outlet /> : <Navigate to="/" replace />;
}
