import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button } from '@mantine/core';
import { StatusBadge } from '../../domain/StatusBadge/StatusBadge';
import { PageHeader } from './PageHeader';

const meta = {
  title: 'Layout/PageHeader',
  component: PageHeader,
  args: {
    title: 'Orden de despacho D-0031',
    description: 'Venta V-0102, Constructora Andina, Entrega completa',
  },
} satisfies Meta<typeof PageHeader>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Basico: Story = {};
export const ConRutaEstadoYAcciones: Story = {
  args: {
    breadcrumbs: [{ label: 'Despachos', href: '/despachos' }, { label: 'D-0031' }],
    status: <StatusBadge entity="guia" value="Emitida" />,
    actions: (
      <>
        <Button variant="default">Cancelar</Button>
        <Button>Confirmar salida</Button>
      </>
    ),
  },
};
