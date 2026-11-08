import type { Meta, StoryObj } from '@storybook/react-vite';
import { MovementBadge } from './MovementBadge';

const meta = { title: 'Dominio/MovementBadge', component: MovementBadge } satisfies Meta<
  typeof MovementBadge
>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Entrada: Story = { args: { tipo: 'Entrada', cantidad: 100 } };
export const Salida: Story = { args: { tipo: 'Salida', cantidad: 60 } };
export const SinCantidad: Story = { args: { tipo: 'Entrada' } };
