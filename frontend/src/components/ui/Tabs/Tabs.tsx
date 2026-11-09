import { Badge, Tabs as MantineTabs } from '@mantine/core';
import { Icon, type IconProps } from '../Icon/Icon';

export interface TabItem {
  value: string;
  label: string;
  count?: number;
  icon?: IconProps['name'];
  disabled?: boolean;
}

export interface TabsProps {
  items: TabItem[];
  value: string;
  onChange: (value: string) => void;
  'aria-label': string;
}

/** Pestañas de una misma vista (hasta cinco). El consumidor presenta el panel con `role="tabpanel"`. */
export function Tabs({ items, value, onChange, 'aria-label': ariaLabel }: TabsProps) {
  return (
    <MantineTabs value={value} onChange={(v) => v && onChange(v)}>
      <MantineTabs.List aria-label={ariaLabel}>
        {items.map(({ value: v, label, count, icon, disabled }) => (
          <MantineTabs.Tab
            key={v}
            value={v}
            disabled={disabled}
            leftSection={icon && <Icon name={icon} size={16} />}
            rightSection={count !== undefined && <Badge size="sm">{count}</Badge>}
          >
            {label}
          </MantineTabs.Tab>
        ))}
      </MantineTabs.List>
    </MantineTabs>
  );
}
