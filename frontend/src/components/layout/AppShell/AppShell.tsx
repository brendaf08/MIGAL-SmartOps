import {
  ActionIcon,
  Anchor,
  AppShell as MantineAppShell,
  Avatar,
  Burger,
  Drawer,
  Group,
  Text,
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { useEffect, useRef, useState, type ReactNode } from 'react';
import { useThemeMode } from '@/theme';
import { Icon } from '../../ui/Icon/Icon';
import { NavMenu, type NavGroup } from './NavMenu';
import classes from './AppShell.module.css';

export interface AppShellProps {
  nav: NavGroup[];
  /** Rol del usuario autenticado; filtra los ítems con `roles`. */
  role?: string;
  current?: string;
  onNavigate: (key: string) => void;
  user: { name: string; role: string };
  onLogout: () => void;
  /** Logo de la marca (URL de la imagen), sin recolorear ni deformar. */
  logo: string;
  appName: string;
  subtitle?: string;
  /** Contenido de la cabecera a partir de tablet (por ejemplo, la ruta). */
  header?: ReactNode;
  children: ReactNode;
}

/**
 * Estructura de la aplicación. Escritorio: barra lateral de 264 px. Tablet: barra de íconos de 72 px.
 * Teléfono: panel deslizable (Drawer) con la misma navegación y cabecera compacta de 56 px.
 */
export function AppShell({
  nav,
  role,
  current,
  onNavigate,
  user,
  onLogout,
  logo,
  appName,
  subtitle,
  header,
  children,
}: AppShellProps) {
  const [open, { toggle, close: hide }] = useDisclosure();
  const [burger, setBurger] = useState<HTMLButtonElement | null>(null);
  const close = hide;
  // Al cerrarse el panel (Escape, velo o ítem), el foco regresa al botón de menú.
  const wasOpen = useRef(false);
  useEffect(() => {
    if (wasOpen.current && !open) burger?.focus();
    wasOpen.current = open;
  }, [open, burger]);
  const { scheme, toggle: toggleScheme } = useThemeMode();
  const brand = (
    <div className={classes.brand}>
      <img src={logo} alt="" width={44} height={44} />
      <div>
        <Text fw={700}>{appName}</Text>
        {subtitle && <Text className={classes.subtitle}>{subtitle}</Text>}
      </div>
    </div>
  );
  const menu = (onSelect?: () => void) => (
    <NavMenu
      nav={nav}
      role={role}
      current={current}
      onNavigate={(key) => {
        onSelect?.();
        onNavigate(key);
      }}
    />
  );

  return (
    <MantineAppShell
      layout="alt"
      header={{ height: { base: 56, sm: 64 } }}
      navbar={{ width: { sm: 72, md: 264 }, breakpoint: 'sm', collapsed: { mobile: true } }}
    >
      <Anchor href="#contenido" className={classes.skip}>
        Saltar al contenido
      </Anchor>

      <MantineAppShell.Header className={classes.header}>
        <Burger
          ref={setBurger}
          hiddenFrom="sm"
          opened={open}
          onClick={toggle}
          className={classes.burger}
          aria-label="Abrir menú"
          aria-controls="panel-navegacion"
          aria-expanded={open}
        />
        <Text hiddenFrom="sm" fw={700} className={classes.appName}>
          {appName}
        </Text>
        <div className={classes.slot}>{header}</div>
        <ActionIcon
          aria-label={scheme === 'dark' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
          onClick={toggleScheme}
        >
          <Icon name={scheme === 'dark' ? 'sun' : 'moon'} />
        </ActionIcon>
        <Group gap="xs" wrap="nowrap">
          <Avatar color="brand" radius="xl" name={user.name} />
          <div className={classes.userText}>
            <Text fw={600}>{user.name}</Text>
            <Text size="xs" c="dimmed">
              {user.role}
            </Text>
          </div>
        </Group>
        <ActionIcon aria-label="Cerrar sesión" onClick={onLogout}>
          <Icon name="logout" />
        </ActionIcon>
      </MantineAppShell.Header>

      <MantineAppShell.Navbar className={classes.navbar}>
        {brand}
        {menu()}
      </MantineAppShell.Navbar>

      <Drawer
        id="panel-navegacion"
        opened={open}
        onClose={close}
        returnFocus
        hiddenFrom="sm"
        size="min(var(--drawer-width), 86vw)"
        withCloseButton={false}
        classNames={{ content: classes.drawer, body: classes.drawerBody }}
        aria-label="Navegación principal"
      >
        {brand}
        {menu(close)}
      </Drawer>

      <MantineAppShell.Main id="contenido" className={classes.main}>
        <div className={classes.content}>{children}</div>
      </MantineAppShell.Main>
    </MantineAppShell>
  );
}
