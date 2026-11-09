/** Flujos de origen desde los que se procesa un documento; el parámetro `destino` los conserva. */
export const DESTINOS = ['cotizacion', 'venta', 'recepcion'] as const;

export type Destino = (typeof DESTINOS)[number];

/** Lee el parámetro `destino` de la URL; un valor desconocido se trata como ausente. */
export function leerDestino(valor: string | null): Destino | undefined {
  return DESTINOS.find((d) => d === valor);
}

const RUTA_DESTINO: Record<Destino, string> = {
  cotizacion: '/cotizaciones/nueva',
  venta: '/ventas/nueva',
  recepcion: '/recepciones/nueva',
};

/** Pantalla del flujo de origen; sin destino, el documento vuelve a la lista de documentos. */
export const rutaDestino = (destino?: Destino) => (destino ? RUTA_DESTINO[destino] : '/documentos');
