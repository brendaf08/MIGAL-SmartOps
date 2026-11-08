import { useState } from 'react';
import logo from '@/assets/progresol-logo.png';
import { AppShell } from '@/components/layout/AppShell/AppShell';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { NAV, type Role } from '@/routes/navigation';

/** Sesión provisional hasta incorporar el módulo de seguridad (autenticación y roles). */
const SESSION: { name: string; role: Role } = { name: 'Diego Pinedo', role: 'Almacenero' };

export function App() {
  const [current, setCurrent] = useState('disponibilidad');
  const title =
    NAV.flatMap((group) => group.items).find((item) => item.key === current)?.label ??
    'MIGAL SmartOps';

  return (
    <AppShell
      nav={NAV}
      role={SESSION.role}
      current={current}
      onNavigate={setCurrent}
      user={SESSION}
      onLogout={() => setCurrent('disponibilidad')}
      logo={logo}
      appName="MIGAL SmartOps"
      subtitle="Ferretería Progresol Migal"
    >
      <PageHeader title={title} />
    </AppShell>
  );
}
