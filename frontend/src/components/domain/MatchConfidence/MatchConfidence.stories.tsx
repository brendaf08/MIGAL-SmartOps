import type { Meta, StoryObj } from '@storybook/react-vite';
import { MatchConfidence } from './MatchConfidence';

const meta = {
  title: 'Dominio/MatchConfidence',
  component: MatchConfidence,
  args: { score: 0.94 },
} satisfies Meta<typeof MatchConfidence>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Alta: Story = {};
export const Media: Story = { args: { score: 0.72 } };
export const Baja: Story = { args: { score: 0.31 } };
export const BajaCompacta: Story = { args: { score: 0.31, compact: true } };
