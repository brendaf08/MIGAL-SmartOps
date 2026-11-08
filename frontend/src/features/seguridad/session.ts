import { createContext, useContext } from 'react';
import type { User } from './roles';

export interface Session {
  user: User | null;
  login: (user: User) => void;
  logout: () => void;
}

export const SessionContext = createContext<Session | null>(null);

/** Sesión del usuario autenticado. */
export function useSession(): Session {
  const session = useContext(SessionContext);
  if (!session) throw new Error('useSession requiere SessionProvider.');
  return session;
}
