import type { Meta, StoryObj } from '@storybook/react-vite';
import { NAV } from '@/routes/navigation';
import { AppShell } from './AppShell';

const ROL = 'Almacenero';

function Demo() {
  return (
    <AppShell
      nav={NAV}
      role={ROL}
      current="disponibilidad"
      onNavigate={() => undefined}
      user={{ name: 'Ana Ruiz', role: ROL }}
      onLogout={() => undefined}
      logo="/vite.svg"
      appName="MIGAL SmartOps"
      subtitle="Ferretería Progresol Migal"
    >
      <h1>Contenido de la página</h1>
    </AppShell>
  );
}

const meta = {
  title: 'Layout/AppShell',
  component: AppShell,
  parameters: { layout: 'fullscreen' },
} satisfies Meta<typeof AppShell>;
export default meta;
type Story = StoryObj<typeof meta>;

const base = {
  nav: NAV,
  onNavigate: () => undefined,
  user: { name: 'Ana Ruiz', role: ROL },
  onLogout: () => undefined,
  logo: '/vite.svg',
  appName: 'MIGAL SmartOps',
  children: null,
};

export const Estandar: Story = { args: base, render: () => <Demo /> };
