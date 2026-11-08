import type { Meta, StoryObj } from '@storybook/react-vite';
import { Icon } from './Icon';
import { ICONS, type IconName } from './icons';

const meta = { title: 'UI/Icon', component: Icon, args: { name: 'kardex' } } satisfies Meta<
  typeof Icon
>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Individual: Story = { args: { size: 24, title: 'Kardex' } };

export const Catalogo: Story = {
  render: () => (
    <ul
      style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))',
        gap: 12,
        padding: 0,
        listStyle: 'none',
      }}
    >
      {(Object.keys(ICONS) as IconName[]).map((name) => (
        <li key={name} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Icon name={name} size={24} />
          <code>{name}</code>
        </li>
      ))}
    </ul>
  ),
};
