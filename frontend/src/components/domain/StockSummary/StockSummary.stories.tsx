import type { Meta, StoryObj } from '@storybook/react-vite';
import { StockSummary } from './StockSummary';

const meta = {
  title: 'Dominio/StockSummary',
  component: StockSummary,
  args: { fisico: 500, reservado: 120, minimo: 200, unidad: 'bolsa' },
} satisfies Meta<typeof StockSummary>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Normal: Story = {};
export const EnElMinimo: Story = { args: { fisico: 260, reservado: 80 } };
export const Critico: Story = { args: { fisico: 90, reservado: 60 } };
export const Compacta: Story = { args: { compact: true } };
