import type { Meta, StoryObj } from '@storybook/react-vite';
import { useState } from 'react';
import { Tabs } from './Tabs';

function Demo() {
  const [value, setValue] = useState('todos');
  return (
    <Tabs
      aria-label="Filtro de disponibilidad"
      value={value}
      onChange={setValue}
      items={[
        { value: 'todos', label: 'Todos', count: 48 },
        { value: 'alerta', label: 'En alerta', count: 12, icon: 'alert-triangle' },
      ]}
    />
  );
}

const meta = { title: 'UI/Tabs', component: Tabs } satisfies Meta<typeof Tabs>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Interactivas: Story = {
  args: { 'aria-label': 'Filtro', value: 'todos', items: [], onChange: () => undefined },
  render: () => <Demo />,
};
