import type { Meta, StoryObj } from '@storybook/react-vite';
import { useState } from 'react';
import { Pagination } from './Pagination';

function Demo() {
  const [page, setPage] = useState(2);
  return <Pagination page={page} total={48} pageSize={20} onChange={setPage} />;
}

const meta = { title: 'UI/Pagination', component: Pagination } satisfies Meta<typeof Pagination>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Interactiva: Story = {
  args: { page: 2, total: 48, onChange: () => undefined },
  render: () => <Demo />,
};
