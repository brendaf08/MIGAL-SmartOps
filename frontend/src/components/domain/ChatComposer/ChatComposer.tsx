import { ActionIcon, Button, Group, Text, Textarea } from '@mantine/core';
import { useState } from 'react';
import { Icon } from '../../ui/Icon/Icon';
import classes from './ChatComposer.module.css';

export interface ChatComposerProps {
  onSend: (text: string) => void;
  /** Hasta tres consultas frecuentes, adaptadas al rol. */
  suggestions?: string[];
  /** Mientras el agente responde, el campo se deshabilita. */
  disabled?: boolean;
  placeholder?: string;
}

/** Entrada del chat: Enter envía, Mayús+Enter inserta un salto de línea. Fija al pie en teléfono. */
export function ChatComposer({
  onSend,
  suggestions = [],
  disabled,
  placeholder = 'Escribir una consulta',
}: ChatComposerProps) {
  const [value, setValue] = useState('');
  const text = value.trim();

  const send = (message: string) => {
    if (!message || disabled) return;
    onSend(message);
    setValue('');
  };

  return (
    <div className={classes.root}>
      {suggestions.length > 0 && (
        <Group gap="xs">
          {suggestions.slice(0, 3).map((s) => (
            <Button
              key={s}
              variant="default"
              size="compact-md"
              disabled={disabled}
              onClick={() => send(s)}
            >
              {s}
            </Button>
          ))}
        </Group>
      )}
      <Group align="flex-end" wrap="nowrap" gap="xs">
        <Textarea
          className={classes.field}
          aria-label="Consulta al agente"
          placeholder={placeholder}
          autosize
          minRows={1}
          maxRows={5}
          disabled={disabled}
          value={value}
          onChange={(e) => setValue(e.currentTarget.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              send(text);
            }
          }}
        />
        <ActionIcon
          aria-label="Enviar consulta"
          size="xl"
          variant="filled"
          disabled={disabled || !text}
          onClick={() => send(text)}
        >
          <Icon name="send" />
        </ActionIcon>
      </Group>
      <Text size="xs" c="dimmed">
        El agente solo consulta información según los permisos de su rol; no modifica datos.
      </Text>
    </div>
  );
}
