import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button, Text } from '@mantine/core';
import { ActionBar } from './ActionBar';

const meta = {
  title: 'UI/ActionBar',
  component: ActionBar,
  parameters: { layout: 'fullscreen' },
} satisfies Meta<typeof ActionBar>;
export default meta;
type Story = StoryObj<typeof meta>;

const acciones = (bloqueada: boolean) => (
  <>
    <Button variant="default">Cancelar</Button>
    <Button disabled={bloqueada}>Confirmar recepción</Button>
  </>
);

export const ConBloqueo: Story = {
  args: {
    summary: <Text fw={600}>3 líneas: 1 completa, 1 parcial, 1 sin asociar</Text>,
    reason: 'Falta asociar 1 línea a un producto del catálogo.',
    children: acciones(true),
  },
};

export const Lista: Story = {
  args: {
    summary: <Text fw={600}>3 líneas: 2 completas, 1 parcial</Text>,
    children: acciones(false),
  },
};
