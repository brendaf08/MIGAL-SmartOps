import type { Meta, StoryObj } from '@storybook/react-vite';
import { StatusBadge } from './StatusBadge';

const meta = {
  title: 'Dominio/StatusBadge',
  component: StatusBadge,
  args: { entity: 'recepcion', value: 'Validada' },
} satisfies Meta<typeof StatusBadge>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Insignia: Story = {};
export const Sello: Story = { args: { variant: 'sello' } };
export const Advertencia: Story = { args: { entity: 'stock', value: 'Minimo' } };
export const Peligro: Story = { args: { entity: 'stock', value: 'Critico' } };
export const Neutro: Story = { args: { entity: 'registro', value: 'Inactivo' } };
