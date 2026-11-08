import type { Meta, StoryObj } from '@storybook/react-vite';
import { useState } from 'react';
import { LineasEditor } from './LineasEditor';
import type { LineaEditable } from './calculos';

const productos = [
  { sku: 'CEM-001', nombre: 'Cemento Sol tipo I, bolsa 42.5 kg' },
  { sku: 'FIE-003', nombre: 'Fierro corrugado 1/2 in x 9 m' },
  { sku: 'LAD-002', nombre: 'Ladrillo King Kong 18 huecos' },
];

const iniciales: LineaEditable[] = [
  { id: 1, sku: 'CEM-001', cantidad: 200, precio: 28.9 },
  { id: 2, sku: 'FIE-003', cantidad: 60, precio: 42.5 },
];

function Demo({
  conPrecio,
  conDescuento,
  lineasIniciales = iniciales,
}: {
  conPrecio: boolean;
  conDescuento?: boolean;
  lineasIniciales?: LineaEditable[];
}) {
  const [lineas, setLineas] = useState(lineasIniciales);
  const [descuento, setDescuento] = useState(0);
  return (
    <LineasEditor
      lineas={lineas}
      onChange={setLineas}
      productos={productos}
      conPrecio={conPrecio}
      conDescuento={conDescuento}
      descuento={descuento}
      onDescuentoChange={setDescuento}
    />
  );
}

const meta = {
  title: 'UI/LineasEditor',
  component: LineasEditor,
  args: { lineas: [], onChange: () => undefined, productos },
} satisfies Meta<typeof LineasEditor>;
export default meta;
type Story = StoryObj<typeof meta>;

export const SoloCantidades: Story = { render: () => <Demo conPrecio={false} /> };
export const ConPrecios: Story = { render: () => <Demo conPrecio /> };
export const ConDescuento: Story = { render: () => <Demo conPrecio conDescuento /> };
export const Vacio: Story = { render: () => <Demo conPrecio lineasIniciales={[]} /> };
export const ConError: Story = {
  args: { error: 'Agregar al menos un producto.' },
  render: (args) => <LineasEditor {...args} conPrecio />,
};
