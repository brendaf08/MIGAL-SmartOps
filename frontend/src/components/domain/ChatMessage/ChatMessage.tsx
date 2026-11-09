import { Loader, Text } from '@mantine/core';
import type { ReactNode } from 'react';
import { cx } from '@/lib/cx';
import { Icon } from '../../ui/Icon/Icon';
import classes from './ChatMessage.module.css';

export interface ChatTool {
  label: string;
  status: 'running' | 'done';
}

export interface ChatMessageProps {
  author: 'user' | 'agent';
  name?: string;
  time?: string;
  /** Consultas al backend que realizó el agente (tool calling). */
  tools?: ChatTool[];
  /** Respuesta sin información suficiente: no inventa cantidades ni ubicaciones (RF-IA-08). */
  uncertain?: boolean;
  children: ReactNode;
}

/** Mensaje de la conversación con el agente consultivo; el agente nunca ofrece acciones que modifiquen datos. */
export function ChatMessage({
  author,
  name = 'Usted',
  time,
  tools,
  uncertain,
  children,
}: ChatMessageProps) {
  const agent = author === 'agent';

  return (
    <div className={cx(classes.root, agent ? classes.agent : classes.user)}>
      {agent && (
        <span className={classes.avatar} aria-hidden="true">
          <Icon name="sparkles" size={20} />
        </span>
      )}
      <div className={classes.main}>
        <Text size="xs" c="dimmed">
          {agent ? 'Agente MIGAL' : name}
          {time && `, ${time}`}
        </Text>
        {tools && tools.length > 0 && (
          <ul className={classes.tools}>
            {tools.map((tool) => (
              <li key={tool.label}>
                {tool.status === 'running' ? <Loader size={14} /> : <Icon name="check" size={16} />}
                {tool.label}
              </li>
            ))}
          </ul>
        )}
        <div className={cx(classes.bubble, uncertain && classes.uncertain)}>
          {uncertain && <Icon name="alert-triangle" size={16} />}
          <span>{children}</span>
        </div>
      </div>
    </div>
  );
}
