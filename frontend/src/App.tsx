// ARCHIVO DE INTEGRACIÓN: no se modifica al agregar pantallas.
// Las pantallas llegan solas desde src/modules/<modulo>/index.tsx vía routes.tsx.
import type { ReactNode } from 'react';
import { BrowserRouter, Routes, Route, Navigate, NavLink, matchPath, useLocation } from 'react-router-dom';
import { LogOut, Circle } from 'lucide-react';
import { AuthProvider, useAuth } from './shared/auth/AuthContext';
import { puedeVer } from './shared/auth/permisos';
import type { CodigoPantalla, RutaModulo } from './shared/tipos';
import { grupos, iconos, nombreRol } from './shared/navegacion';
import Logo from './shared/components/Logo';
import { todasLasRutas } from './routes';

function MenuLateral() {
  const { rol } = useAuth();
  const visibles = todasLasRutas.filter((r) => r.enMenu && puedeVer(rol, r.codigo));
  const porCodigo = new Map(visibles.map((r) => [r.codigo, r]));
  const agrupados = grupos
    .map((g) => ({ nombre: g.nombre, rutas: g.codigos.map((c) => porCodigo.get(c)).filter(Boolean) as RutaModulo[] }))
    .filter((g) => g.rutas.length > 0);
  const enGrupo = new Set(grupos.flatMap((g) => g.codigos));
  const otros = visibles.filter((r) => !enGrupo.has(r.codigo));
  if (otros.length) agrupados.push({ nombre: 'Otros', rutas: otros });

  return (
    <aside className="no-imprimir sticky top-0 h-screen w-64 shrink-0 bg-lateral text-white flex flex-col">
      <div className="px-5 h-16 flex items-center border-b border-white/10">
        <Logo claro />
      </div>
      <nav className="menu-lateral flex-1 overflow-y-auto px-3 py-4 space-y-5" aria-label="Menú principal">
        {agrupados.map((g) => (
          <div key={g.nombre}>
            <p className="px-3 mb-2 text-[11px] font-semibold uppercase tracking-widest text-white/40">{g.nombre}</p>
            <ul className="space-y-0.5">
              {g.rutas.map((r) => {
                const Icono = iconos[r.codigo] ?? Circle;
                return (
                  <li key={r.codigo}>
                    <NavLink to={r.path} end
                      className={({ isActive }) => `relative min-h-11 px-3 rounded-lg flex items-center gap-3 font-medium transition-colors
                        ${isActive
                          ? 'bg-white/10 text-white before:absolute before:-left-3 before:top-2.5 before:bottom-2.5 before:w-0.5 before:rounded-r before:bg-primario-suave'
                          : 'text-white/65 hover:bg-white/5 hover:text-white'}`}>
                      <Icono size={19} aria-hidden />
                      {r.titulo}
                    </NavLink>
                  </li>
                );
              })}
            </ul>
          </div>
        ))}
      </nav>
      <p className="px-5 py-3 text-xs text-white/40 border-t border-white/10">Ferretería MIGAL · v1.0</p>
    </aside>
  );
}

function BarraSuperior() {
  const { usuario, rol, cerrarSesion } = useAuth();
  const { pathname } = useLocation();
  const actual = todasLasRutas.find((r) => matchPath({ path: r.path, end: true }, pathname));
  const grupo = actual && grupos.find((g) => g.codigos.includes(actual.codigo))?.nombre;
  if (!usuario) return null;
  const iniciales = `${usuario.nombres[0]}${usuario.apellidos[0]}`;

  return (
    <header className="no-imprimir sticky top-0 z-10 h-16 bg-superficie/80 backdrop-blur-md shadow-[0_1px_0_rgb(0_0_0/0.04)] px-8 flex items-center justify-between">
      <div className="text-sm">
        <span className="text-secundario">{grupo ?? 'Inicio'}</span>
        {actual && <><span className="mx-2 text-borde">/</span><span className="font-semibold">{actual.titulo}</span></>}
      </div>
      <div className="flex items-center gap-4">
        <div className="text-right leading-tight hidden md:block">
          <p className="font-semibold">{usuario.nombres} {usuario.apellidos}</p>
          <p className="text-sm text-secundario">{nombreRol[rol ?? ''] ?? rol}</p>
        </div>
        <span className="grid size-10 place-items-center rounded-full bg-primario-suave text-primario-oscuro font-bold" aria-hidden>
          {iniciales}
        </span>
        <button onClick={cerrarSesion} title="Cerrar sesión" aria-label="Cerrar sesión"
          className="grid size-11 place-items-center rounded-lg text-secundario hover:bg-fondo hover:text-error transition-colors">
          <LogOut size={20} aria-hidden />
        </button>
      </div>
    </header>
  );
}

function Marco({ children }: { children: ReactNode }) {
  const { usuario } = useAuth();
  if (!usuario) return <>{children}</>; // login a pantalla completa
  return (
    <div className="flex min-h-screen">
      <MenuLateral />
      <div className="flex-1 min-w-0 flex flex-col">
        <BarraSuperior />
        <main className="flex-1">
          <div className="mx-auto max-w-7xl p-8 lg:p-10">{children}</div>
        </main>
      </div>
    </div>
  );
}

function Protegida({ codigo, children }: { codigo: CodigoPantalla; children: ReactNode }) {
  const { rol } = useAuth();
  // Con sesión iniciada, el login no se vuelve a mostrar (por ejemplo al retroceder)
  if (codigo === 'P01' && rol) return <Navigate to="/agente" replace />;
  if (puedeVer(rol, codigo)) return <>{children}</>;
  return <Navigate to={rol ? '/agente' : '/login'} replace />;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Marco>
          <Routes>
            <Route path="/" element={<Navigate to="/login" replace />} />
            {todasLasRutas.map((r) => (
              <Route key={r.codigo} path={r.path} element={<Protegida codigo={r.codigo}>{r.elemento}</Protegida>} />
            ))}
            <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
        </Marco>
      </BrowserRouter>
    </AuthProvider>
  );
}
