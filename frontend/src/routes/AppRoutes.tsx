import { Navigate, Route, Routes } from 'react-router';
import { EmptyState } from '@/components/ui/EmptyState/EmptyState';
import { RequireAuth, RequireRole } from '@/features/seguridad/guards';
import { LoginPage } from '@/features/seguridad/LoginPage';
import { useSession } from '@/features/seguridad/session';
import { NAV } from './navigation';
import type { RutaModulo } from './modulos/tipos';
import { PendientePage } from './PendientePage';
import { ShellLayout } from './ShellLayout';

const ITEMS = NAV.flatMap((group) => group.items);

/**
 * Rutas de cada módulo, leídas de `modulos/<clave>.tsx`. La clave es el nombre del archivo hasta el primer punto,
 * de modo que `ventas.registro.tsx` aporta rutas al módulo `ventas`. Cada archivo pertenece a una sola rama de trabajo.
 */
const ARCHIVOS = import.meta.glob<{ RUTAS: RutaModulo[] }>('./modulos/*.tsx', { eager: true });
const RUTAS_POR_MODULO = Object.entries(ARCHIVOS).reduce<Record<string, RutaModulo[]>>(
  (acc, [archivo, modulo]) => {
    const clave = archivo.replace('./modulos/', '').split('.')[0] ?? '';
    acc[clave] = [...(acc[clave] ?? []), ...modulo.RUTAS];
    return acc;
  },
  {},
);

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
          {ITEMS.map((item) => {
            const rutas = RUTAS_POR_MODULO[item.key] ?? [];
            const principal = rutas.some((r) => r.path === '');
            return (
              <Route key={item.key} element={<RequireRole roles={item.roles} />}>
                {!principal && (
                  <Route path={item.key} element={<PendientePage title={item.label} />} />
                )}
                {rutas.map((ruta) => {
                  const path = ruta.path ? `${item.key}/${ruta.path}` : item.key;
                  return ruta.roles ? (
                    <Route key={ruta.path} element={<RequireRole roles={ruta.roles} />}>
                      <Route path={path} element={ruta.element} />
                    </Route>
                  ) : (
                    <Route key={ruta.path} path={path} element={ruta.element} />
                  );
                })}
              </Route>
            );
          })}
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
