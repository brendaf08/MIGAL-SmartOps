import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { EmptyState } from '@/components/ui/EmptyState/EmptyState';

/** Módulo cuya pantalla aún no se implementa: conserva el encabezado y la ruta protegida. */
export function PendientePage({ title }: { title: string }) {
  return (
    <>
      <PageHeader title={title} />
      <EmptyState
        title="Módulo en construcción"
        description="Esta pantalla aún no está disponible."
      />
    </>
  );
}
