import { ActionIcon, TextInput, type TextInputProps } from '@mantine/core';
import { Icon } from '../Icon/Icon';

export interface SearchInputProps extends Omit<TextInputProps, 'value' | 'onChange' | 'onSubmit'> {
  value: string;
  onChange: (value: string) => void;
  /** Se invoca con Enter. El consumidor aplica la espera (debounce) antes de consultar al servidor. */
  onSubmit?: (value: string) => void;
}

/** Campo de búsqueda con lupa y botón "Limpiar búsqueda" visible cuando hay texto. */
export function SearchInput({ value, onChange, onSubmit, ...props }: SearchInputProps) {
  return (
    <TextInput
      type="search"
      autoComplete="off"
      spellCheck={false}
      leftSection={<Icon name="search" />}
      rightSection={
        value && (
          <ActionIcon aria-label="Limpiar búsqueda" size="md" onClick={() => onChange('')}>
            <Icon name="close" size={16} />
          </ActionIcon>
        )
      }
      value={value}
      onChange={(e) => onChange(e.currentTarget.value)}
      onKeyDown={(e) => e.key === 'Enter' && onSubmit?.(value)}
      {...props}
    />
  );
}
