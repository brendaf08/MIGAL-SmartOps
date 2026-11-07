import { createContext, useContext, useState, type ReactNode } from 'react';
import type { Rol, Usuario } from '../tipos';
import { usuarios } from '../data/semillas';

const CLAVE_PRUEBA = 'migal123'; // misma contraseña para los 4 usuarios de prueba

interface Sesion {
  usuario: Usuario | null;
  rol: Rol | null;
  iniciarSesion: (username: string, password: string) => boolean;
  cerrarSesion: () => void;
}

const AuthContext = createContext<Sesion | null>(null);

export function AuthProvider({ children, usuarioInicial = null }:
  { children: ReactNode; usuarioInicial?: Usuario | null }) {
  const [usuario, setUsuario] = useState<Usuario | null>(usuarioInicial);

  const iniciarSesion = (username: string, password: string): boolean => {
    const encontrado = usuarios.find((u) => u.username === username && u.estado_activo);
    if (!encontrado || password !== CLAVE_PRUEBA) return false;
    setUsuario(encontrado);
    return true;
  };

  return (
    <AuthContext.Provider value={{ usuario, rol: usuario?.rol ?? null, iniciarSesion, cerrarSesion: () => setUsuario(null) }}>
      {children}
    </AuthContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): Sesion {
  const sesion = useContext(AuthContext);
  if (!sesion) throw new Error('useAuth debe usarse dentro de AuthProvider');
  return sesion;
}
