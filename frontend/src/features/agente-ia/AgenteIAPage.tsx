import { Badge } from '@mantine/core';
import { useMutation } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import { ChatComposer } from '@/components/domain/ChatComposer/ChatComposer';
import { ChatMessage, type ChatMessageProps } from '@/components/domain/ChatMessage/ChatMessage';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { Icon } from '@/components/ui/Icon/Icon';
import { useSession } from '@/features/seguridad/session';
import { consultarAgente } from './api';
import classes from './AgenteIAPage.module.css';

interface Mensaje {
  id: number;
  author: ChatMessageProps['author'];
  time: string;
  text: string;
  consultas?: string[];
  incierta?: boolean;
}

const SUGERENCIAS = ['Productos en stock crítico', 'Disponibilidad de ladrillo King Kong'];
const hora = () => new Date().toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });

/** CU-14: consultas en lenguaje natural sobre inventario. El agente solo consulta, según el rol del usuario. */
export function AgenteIAPage() {
  const { user } = useSession();
  const [mensajes, setMensajes] = useState<Mensaje[]>([]);
  const end = useRef<HTMLDivElement>(null);
  const add = (m: Omit<Mensaje, 'id' | 'time'>) =>
    setMensajes((prev) => [...prev, { ...m, id: prev.length, time: hora() }]);

  const consulta = useMutation({
    mutationFn: consultarAgente,
    onSuccess: (r) =>
      add({ author: 'agent', text: r.texto, consultas: r.consultas, incierta: r.incierta }),
    onError: () =>
      add({
        author: 'agent',
        text: 'No se pudo completar la consulta. Intentar de nuevo.',
        incierta: true,
      }),
  });

  const enviar = (text: string) => {
    add({ author: 'user', text });
    consulta.mutate(text);
  };

  useEffect(() => end.current?.scrollIntoView?.({ block: 'end' }), [mensajes, consulta.isPending]);

  return (
    <>
      <PageHeader
        title="Agente de IA"
        description="Consultas en lenguaje natural sobre inventario, productos y ubicación de mercadería."
        status={<Badge leftSection={<Icon name="rol" size={16} />}>Rol: {user?.role}</Badge>}
      />
      <div className={classes.chat}>
        <div
          className={classes.log}
          role="log"
          aria-label="Conversación con el agente"
          aria-live="polite"
        >
          {mensajes.map((m) => (
            <ChatMessage
              key={m.id}
              author={m.author}
              name={user?.name}
              time={m.time}
              uncertain={m.incierta}
              tools={m.consultas?.map((label) => ({ label, status: 'done' }))}
            >
              {m.text}
            </ChatMessage>
          ))}
          {consulta.isPending && (
            <ChatMessage
              author="agent"
              tools={[{ label: 'Consultando disponibilidad', status: 'running' }]}
            >
              Revisando existencias…
            </ChatMessage>
          )}
          <div ref={end} />
        </div>
        <ChatComposer onSend={enviar} suggestions={SUGERENCIAS} disabled={consulta.isPending} />
      </div>
    </>
  );
}
