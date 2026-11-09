import { useState, type ReactNode } from 'react';
import type { User } from './roles';
import { SessionContext } from './session';

const KEY = 'migal-session';

function restore(): User | null {
  try {
    return JSON.parse(window.sessionStorage.getItem(KEY) ?? 'null') as User | null;
  } catch {
    return null;
  }
}

/** Conserva el usuario durante la pestaña. La autorización real la aplica el backend. */
export function SessionProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(restore);

  const login = (next: User) => {
    window.sessionStorage.setItem(KEY, JSON.stringify(next));
    setUser(next);
  };
  const logout = () => {
    window.sessionStorage.removeItem(KEY);
    setUser(null);
  };

  return <SessionContext value={{ user, login, logout }}>{children}</SessionContext>;
}
