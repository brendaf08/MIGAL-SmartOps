import { Alert } from '../Alert/Alert';
import { DataTable, type DataTableProps } from '../DataTable/DataTable';

interface QueryTableProps<T> extends Omit<DataTableProps<T>, 'rows' | 'loading'> {
  /** Resultado de una consulta de TanStack Query. */
  query: { data?: T[]; isLoading: boolean; isError: boolean };
  /** Título del aviso cuando la consulta falla. */
  errorTitle: string;
}

/** Tabla alimentada por una consulta al servidor: esqueletos al cargar y aviso con salida si falla. */
export function QueryTable<T>({ query, errorTitle, ...props }: QueryTableProps<T>) {
  if (query.isError) {
    return (
      <Alert tone="danger" title={errorTitle}>
        Verificar la conexión e intentar de nuevo.
      </Alert>
    );
  }
  return <DataTable {...props} rows={query.data ?? []} loading={query.isLoading} />;
}
