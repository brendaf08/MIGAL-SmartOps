import { Badge, NavLink, Text } from '@mantine/core';
import type { ReactNode } from 'react';
import { Icon, type IconProps } from '../../ui/Icon/Icon';
import classes from './AppShell.module.css';

export interface NavItem {
  key: string;
  label: string;
  icon: IconProps['name'];
  /** Roles con acceso; sin `roles`, el ítem es visible para todos. */
  roles?: string[];
  badge?: ReactNode;
}

export interface NavGroup {
  label: string;
  items: NavItem[];
}

interface NavMenuProps {
  nav: NavGroup[];
  role?: string;
  current?: string;
  onNavigate: (key: string) => void;
}

/** Grupos de navegación filtrados por rol. Se reutiliza en la barra lateral y en el panel del teléfono. */
export function NavMenu({ nav, role, current, onNavigate }: NavMenuProps) {
  return (
    <nav aria-label="Navegación principal">
      {nav.map((group) => {
        const items = group.items.filter(
          (item) => !item.roles || (role && item.roles.includes(role)),
        );
        if (items.length === 0) return null;
        return (
          <div key={group.label} className={classes.group}>
            <Text className={classes.groupLabel}>{group.label}</Text>
            {items.map((item) => (
              <NavLink
                key={item.key}
                classNames={{
                  root: classes.link,
                  label: classes.linkLabel,
                  section: classes.linkSection,
                }}
                component="button"
                label={item.label}
                title={item.label}
                leftSection={<Icon name={item.icon} />}
                rightSection={item.badge && <Badge>{item.badge}</Badge>}
                active={item.key === current}
                aria-current={item.key === current ? 'page' : undefined}
                onClick={() => onNavigate(item.key)}
              />
            ))}
          </div>
        );
      })}
    </nav>
  );
}
