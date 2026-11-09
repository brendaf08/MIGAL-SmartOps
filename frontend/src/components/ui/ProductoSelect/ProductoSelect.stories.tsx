import type { Meta, StoryObj } from '@storybook/react-vite';
import { useState } from 'react';
import { MoneyInput } from '../MoneyInput/MoneyInput';
import { ProductoSelect } from './ProductoSelect';

const productos = [
  { sku: 'CEM-001', nombre: 'Cemento Sol tipo I, bolsa 42.5 kg' },
  { sku: 'FIE-003', nombre: 'Fierro corrugado 1/2 in x 9 m' },
  { sku: 'LAD-002', nombre: 'Ladrillo King Kong 18 huecos' },
];

function Demo({ sugeridos }: { sugeridos?: string[] }) {
  const [sku, setSku] = useState<string | null>(null);
  return (
    <ProductoSelect
      label="Producto"
      productos={productos}
      sugeridos={sugeridos}
      value={sku}
      onChange={setSku}
    />
  );
}

const meta = {
  title: 'UI/ProductoSelect',
  component: ProductoSelect,
  args: { productos, value: null, onChange: () => undefined, label: 'Producto' },
} satisfies Meta<typeof ProductoSelect>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Basico: Story = { render: () => <Demo /> };
export const ConSugeridos: Story = { render: () => <Demo sugeridos={['LAD-002']} /> };
export const ConError: Story = { args: { error: 'Seleccionar un producto.' } };

export const ImporteEnSoles: StoryObj<typeof MoneyInput> = {
  render: () => <MoneyInput label="Precio de venta" defaultValue={1250} />,
};
