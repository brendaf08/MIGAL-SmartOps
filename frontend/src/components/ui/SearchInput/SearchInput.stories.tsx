import type { Meta, StoryObj } from '@storybook/react-vite';
import { useState } from 'react';
import { SearchInput } from './SearchInput';

function Demo() {
  const [value, setValue] = useState('cemento');
  return (
    <SearchInput
      aria-label="Buscar producto"
      placeholder="Nombre o SKU"
      value={value}
      onChange={setValue}
    />
  );
}

const meta = { title: 'UI/SearchInput', component: SearchInput } satisfies Meta<typeof SearchInput>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Interactivo: Story = {
  args: { value: '', onChange: () => undefined },
  render: () => <Demo />,
};
