import type { Meta, StoryObj } from '@storybook/react-vite';
import { StockLocations } from './StockLocations';

const meta = {
  title: 'Dominio/StockLocations',
  component: StockLocations,
  args: {
    unidad: 'bolsa',
    ubicaciones: [
      { nombre: 'Tienda principal', cantidad: 40 },
      { nombre: 'Almacén 1', cantidad: 300, reservado: 100 },
      { nombre: 'Almacén 2', cantidad: 0 },
    ],
  },
} satisfies Meta<typeof StockLocations>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Plano: Story = {};
export const Compacta: Story = { args: { compact: true } };
