// Login PROVISIONAL. Gabriel puede reemplazarlo manteniendo este nombre de archivo.
import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Eye, EyeOff, LogIn, ShieldCheck, Boxes, ScanText } from 'lucide-react';
import { useAuth } from '../../../shared/auth/AuthContext';
import { Boton, Campo } from '../../../shared/components/ui';
import Logo from '../../../shared/components/Logo';

const beneficios = [
  { icono: Boxes, texto: 'Stock en tiempo real por tienda y almacén' },
  { icono: ScanText, texto: 'Pedidos y guías leídos automáticamente' },
  { icono: ShieldCheck, texto: 'Acceso según el rol de cada colaborador' },
];

export default function P01Login() {
  const { iniciarSesion } = useAuth();
  const navegar = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [verClave, setVerClave] = useState(false);
  const [error, setError] = useState('');

  const enviar = (e: FormEvent) => {
    e.preventDefault();
    // replace: el login no queda en el historial, así «Atrás» no regresa a él
    if (iniciarSesion(username.trim(), password)) navegar('/agente', { replace: true });
    else setError('Usuario o contraseña incorrectos. Verifica tus datos e inténtalo otra vez.');
  };

  return (
    <div className="min-h-screen grid lg:grid-cols-2">
      <aside className="hidden lg:flex flex-col justify-between bg-lateral text-white p-12 relative overflow-hidden">
        <div className="absolute -right-24 -top-24 size-96 rounded-full bg-primario/30 blur-3xl" aria-hidden />
        <div className="absolute -left-16 bottom-0 size-72 rounded-full bg-primario/20 blur-3xl" aria-hidden />
        <Logo claro />
        <div className="relative">
          <h1 className="text-4xl font-extrabold leading-tight tracking-tight">Gestión integral<br />para tu ferretería</h1>
          <p className="mt-4 text-white/70 text-lg max-w-md">Ventas, inventario, compras y despachos en un solo lugar.</p>
          <ul className="mt-10 space-y-4">
            {beneficios.map(({ icono: Icono, texto }) => (
              <li key={texto} className="flex items-center gap-3">
                <span className="grid size-10 place-items-center rounded-lg bg-white/10"><Icono size={20} aria-hidden /></span>
                <span className="text-white/85">{texto}</span>
              </li>
            ))}
          </ul>
        </div>
        <p className="relative text-sm text-white/40">© 2026 Ferretería MIGAL</p>
      </aside>

      <main className="flex items-center justify-center p-6 bg-fondo">
        <div className="w-full max-w-md">
          <div className="lg:hidden mb-8"><Logo /></div>
          <h2 className="text-3xl font-bold tracking-tight">Bienvenido</h2>
          <p className="text-secundario mt-2 mb-8">Ingresa con tu usuario para continuar.</p>

          <form onSubmit={enviar} className="space-y-5" noValidate>
            <Campo etiqueta="Usuario" name="username" autoComplete="username" autoFocus
              value={username} onChange={(e) => { setUsername(e.target.value); setError(''); }} />
            <div className="relative">
              <Campo etiqueta="Contraseña" name="password" type={verClave ? 'text' : 'password'} autoComplete="current-password"
                value={password} onChange={(e) => { setPassword(e.target.value); setError(''); }} />
              <button type="button" onClick={() => setVerClave(!verClave)}
                aria-label={verClave ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                className="absolute right-0 top-7 grid size-11 place-items-center text-secundario hover:text-texto">
                {verClave ? <EyeOff size={20} /> : <Eye size={20} />}
              </button>
            </div>
            {error && <p role="alert" className="rounded-lg bg-error/10 text-error px-4 py-3 text-sm">{error}</p>}
            <Boton type="submit" className="w-full" disabled={!username || !password}>
              <LogIn size={19} aria-hidden /> Ingresar
            </Boton>
          </form>

          <div className="mt-8 rounded-lg border border-dashed border-borde p-4 text-sm text-secundario">
            <p className="font-semibold text-texto mb-1">Usuarios de prueba</p>
            admin · vendedor · almacenero · reponedor — clave <code className="font-mono text-texto">migal123</code>
          </div>
        </div>
      </main>
    </div>
  );
}
