import type { Meta, StoryObj } from '@storybook/react-vite';
import { Card } from '../Card/Card';
import { Columns } from './Columns';

const meta = {
  title: 'UI/Columns',
  component: Columns,
  args: {
    layout: 'main-aside',
    children: [
      <Card key="a" title="Principal">
        Contenido principal.
      </Card>,
      <Card key="b" title="Lateral">
        Contenido lateral.
      </Card>,
    ],
  },
} satisfies Meta<typeof Columns>;
export default meta;
type Story = StoryObj<typeof meta>;

export const PrincipalYLateral: Story = {};
export const LateralYPrincipal: Story = { args: { layout: 'aside-main' } };
export const DosColumnas: Story = { args: { layout: 'two' } };
