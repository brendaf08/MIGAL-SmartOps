import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useState, type ReactNode } from 'react';
import { alExpirarSesion, notificarCierreSesion } from '@/api/cliente';
import type { User } from './roles';
import { cerrarSesion } from './api';
import { SessionContext } from './session';

const KEY = 'migal-session';

function restore(): User | null {
  try {
    return JSON.parse(window.sessionStorage.getItem(KEY) ?? 'null') as User | null;
  } catch {
    return null;
  }
}

/**
 * Conserva el usuario durante la pestaña. La autorización real la aplica el backend. Al cerrar la sesión, o cuando el
 * backend rechaza el token con 401, descarta los datos en caché y el estado de los módulos, para que en un equipo
 * compartido de mostrador el siguiente usuario no vea lo que consultó el anterior.
 */
export function SessionProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(restore);
  const queryClient = useQueryClient();

  const login = (next: User) => {
    window.sessionStorage.setItem(KEY, JSON.stringify(next));
    setUser(next);
  };
  const logout = useCallback(() => {
    window.sessionStorage.removeItem(KEY);
    cerrarSesion();
    notificarCierreSesion();
    queryClient.clear();
    setUser(null);
  }, [queryClient]);

  useEffect(() => {
    alExpirarSesion(logout);
    return () => alExpirarSesion(undefined);
  }, [logout]);

  return <SessionContext value={{ user, login, logout }}>{children}</SessionContext>;
}
