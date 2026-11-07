import { QueryClient } from '@tanstack/react-query';

/**
 * Cliente de consultas compartido. Los datos se consideran vigentes durante 30 segundos para evitar
 * solicitudes repetidas (RNF-06) y las consultas fallidas se reintentan una sola vez.
 */
export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        retry: 1,
        refetchOnWindowFocus: false,
      },
    },
  });
}
