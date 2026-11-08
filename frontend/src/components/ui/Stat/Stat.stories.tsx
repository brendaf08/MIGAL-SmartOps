import type { Meta, StoryObj } from '@storybook/react-vite';
import { Stat } from './Stat';

const meta = {
  title: 'UI/Stat',
  component: Stat,
  args: { label: 'Productos en alerta', value: '12', hint: 'de 48 productos' },
} satisfies Meta<typeof Stat>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Basica: Story = {};
export const SinDatos: Story = { args: { value: undefined, hint: undefined } };
export const ConTono: Story = { args: { tone: 'warning' } };
