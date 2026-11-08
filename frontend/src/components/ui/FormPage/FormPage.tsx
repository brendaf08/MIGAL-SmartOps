import { Button, Stack } from '@mantine/core';
import { useRef, type FormEvent, type ReactNode } from 'react';
import { PageHeader, type PageHeaderProps } from '../../layout/PageHeader/PageHeader';
import { ActionBar } from '../ActionBar/ActionBar';

export interface FormPageProps {
  title: string;
  description?: string;
  breadcrumbs?: PageHeaderProps['breadcrumbs'];
  /** Tarjetas del formulario, apiladas. */
  children: ReactNode;
  /** Resumen junto a las acciones (por ejemplo, "3 productos, total S/ 120.00"). */
  summary?: ReactNode;
  /** Motivo por el que no se puede enviar; visible junto a la acción deshabilitada. */
  reason?: string | null;
  submitLabel: string;
  submitting?: boolean;
  /** `false` deshabilita la acción principal; debe acompañarse de `reason`. */
  canSubmit?: boolean;
  /** Se invoca al enviar; con `form.onSubmit(...)` de Mantine ejecuta además la validación. */
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onCancel: () => void;
}

/**
 * Estructura de un formulario de página completa: encabezado, tarjetas apiladas y barra de acciones al pie con
 * Cancelar y la acción principal. Al enviar con errores, el foco pasa al primer campo inválido.
 */
export function FormPage({
  title,
  description,
  breadcrumbs,
  children,
  summary,
  reason,
  submitLabel,
  submitting = false,
  canSubmit = true,
  onSubmit,
  onCancel,
}: FormPageProps) {
  const formRef = useRef<HTMLFormElement>(null);

  const enviar = (event: FormEvent<HTMLFormElement>) => {
    onSubmit(event);
    // La validación actualiza los errores en el mismo ciclo; el foco se mueve cuando ya están en el DOM.
    setTimeout(
      () => formRef.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus(),
      0,
    );
  };

  return (
    <>
      <PageHeader title={title} description={description} breadcrumbs={breadcrumbs} />
      <form ref={formRef} noValidate onSubmit={enviar} aria-label={title}>
        <Stack gap="lg">{children}</Stack>
        <ActionBar summary={summary} reason={reason} aria-label={`Acciones de ${title}`}>
          <Button variant="default" onClick={onCancel}>
            Cancelar
          </Button>
          <Button type="submit" loading={submitting} disabled={!canSubmit}>
            {submitLabel}
          </Button>
        </ActionBar>
      </form>
    </>
  );
}
