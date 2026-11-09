import type { Meta, StoryObj } from '@storybook/react-vite';
import { StockBar } from './StockBar';

const meta = {
  title: 'Dominio/StockBar',
  component: StockBar,
  args: { libre: 120, reservado: 30, unidad: 'bolsa' },
} satisfies Meta<typeof StockBar>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Basica: Story = {};
export const ConPendiente: Story = { args: { pendiente: 40 } };
export const ConMarcadores: Story = { args: { minimo: 50, reposicion: 80 } };
export const CintaPorEmpaque: Story = {
  args: {
    libre: 640,
    reservado: 120,
    minimo: 200,
    reposicion: 400,
    empaque: { paso: 100, unidad: 'bolsas' },
  },
};
export const Compacta: Story = { args: { compact: true, label: 'Reparto de Cemento Sol tipo I' } };
