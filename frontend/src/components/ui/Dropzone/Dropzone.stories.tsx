import type { Meta, StoryObj } from '@storybook/react-vite';
import { Dropzone } from './Dropzone';
import { FileItem } from './FileItem';

const meta = {
  title: 'UI/Dropzone',
  component: Dropzone,
  args: { onDrop: () => undefined },
} satisfies Meta<typeof Dropzone>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Basica: Story = {};
export const ConCamara: Story = { args: { camera: true } };
export const Deshabilitada: Story = { args: { disabled: true } };

export const ArchivoProcesando: StoryObj<typeof FileItem> = {
  render: () => <FileItem name="guia-proveedor.jpg" size={254000} status="processing" />,
};
export const ArchivoProcesado: StoryObj<typeof FileItem> = {
  render: () => (
    <FileItem
      name="guia-proveedor.jpg"
      size={254000}
      status="processed"
      onRemove={() => undefined}
    />
  ),
};
export const ArchivoConError: StoryObj<typeof FileItem> = {
  render: () => <FileItem name="guia-borrosa.jpg" size={98000} status="error" />,
};
