import { Button, Stack, Title } from '@mantine/core';
import { useThemeMode } from '@/theme';

export function App() {
  const { scheme, toggle } = useThemeMode();

  return (
    <Stack p="xl" align="flex-start">
      <Title order={1}>MIGAL SmartOps</Title>
      <Button onClick={toggle}>
        {scheme === 'dark' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
      </Button>
    </Stack>
  );
}
