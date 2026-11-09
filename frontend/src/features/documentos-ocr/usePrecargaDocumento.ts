import { useQuery } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { useSearchParams } from 'react-router';
import type { LineaEditable } from '@/components/ui/LineasEditor/calculos';
import { obtenerDocumento, type LineaOcr } from './api';

/**
 * Convierte las líneas revisadas del OCR en líneas editables de un formulario. Solo pasan las líneas asociadas a un
 * producto; el precio queda en cero porque el documento no lo trae y debe completarlo el usuario.
 */
export function lineasDeDocumento(lineas: readonly LineaOcr[]): LineaEditable[] {
  return lineas
    .filter((l) => l.sku && l.cantidad > 0)
    .map((l, i) => ({ id: i + 1, sku: l.sku, cantidad: l.cantidad, precio: 0 }));
}

/**
 * Con `?documento=ID` en la dirección (que deja la revisión del OCR al volver al flujo de origen), entrega una sola vez
 * las líneas del documento a `alCargar`. Devuelve si hay un documento por cargar, para avisar al usuario.
 */
export function usePrecargaDocumento(alCargar: (lineas: LineaEditable[]) => void) {
  const [params] = useSearchParams();
  const id = Number(params.get('documento'));
  const habilitado = Number.isInteger(id) && id > 0;
  const entregado = useRef(false);
  const callback = useRef(alCargar);

  const query = useQuery({
    // Clave propia: la revisión del OCR guarda en caché el documento antes de validarlo, con líneas sin asociar.
    queryKey: ['documento-precarga', id],
    gcTime: 0,
    queryFn: () => obtenerDocumento(id),
    enabled: habilitado,
  });

  useEffect(() => {
    callback.current = alCargar;
  });

  useEffect(() => {
    if (query.data && !entregado.current) {
      entregado.current = true;
      callback.current(lineasDeDocumento(query.data.lineas));
    }
  }, [query.data]);

  return { habilitado, cargando: habilitado && query.isLoading, error: query.isError };
}
