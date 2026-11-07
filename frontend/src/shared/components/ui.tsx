// Kit visual compartido. Úsalo en todas las pantallas para que todo se vea igual.
import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react';

// ─── Botón ───────────────────────────────────────────────────────────
type Variante = 'primario' | 'secundario' | 'peligro' | 'fantasma';
const estilosBoton: Record<Variante, string> = {
  primario: 'bg-primario text-white shadow-sm hover:bg-primario-oscuro hover:shadow-md',
  secundario: 'bg-superficie text-texto shadow-sm ring-1 ring-inset ring-black/5 hover:shadow-md',
  peligro: 'bg-error text-white shadow-sm hover:opacity-90 hover:shadow-md',
  fantasma: 'text-primario hover:bg-primario-suave',
};

export function Boton({ variante = 'primario', className = '', ...props }:
  ButtonHTMLAttributes<HTMLButtonElement> & { variante?: Variante }) {
  return (
    <button
      {...props}
      className={`min-h-11 px-4 rounded-lg font-semibold inline-flex items-center justify-center gap-2 transition-all duration-150
        focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primario
        disabled:opacity-50 disabled:cursor-not-allowed ${estilosBoton[variante]} ${className}`}
    />
  );
}

// ─── Campos de formulario ────────────────────────────────────────────
const estiloEntrada = (error?: string) =>
  `min-h-11 w-full rounded-lg border bg-superficie px-3 outline-none transition
   focus:border-primario focus:ring-4 focus:ring-primario/15 ${error ? 'border-error' : 'border-borde-campo/60 hover:border-borde-campo'}`;

function Envoltura({ etiqueta, error, ayuda, id, children }:
  { etiqueta: string; error?: string; ayuda?: string; id: string; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-medium">{etiqueta}</label>
      {children}
      {error ? <p className="text-sm text-error" role="alert">{error}</p>
        : ayuda ? <p className="text-sm text-secundario">{ayuda}</p> : null}
    </div>
  );
}

export function Campo({ etiqueta, error, ayuda, id, ...props }:
  InputHTMLAttributes<HTMLInputElement> & { etiqueta: string; error?: string; ayuda?: string }) {
  const idCampo = id ?? props.name ?? etiqueta;
  return (
    <Envoltura etiqueta={etiqueta} error={error} ayuda={ayuda} id={idCampo}>
      <input id={idCampo} aria-invalid={!!error} {...props} className={estiloEntrada(error)} />
    </Envoltura>
  );
}

export function Selector({ etiqueta, error, ayuda, id, children, ...props }:
  SelectHTMLAttributes<HTMLSelectElement> & { etiqueta: string; error?: string; ayuda?: string }) {
  const idCampo = id ?? props.name ?? etiqueta;
  return (
    <Envoltura etiqueta={etiqueta} error={error} ayuda={ayuda} id={idCampo}>
      <select id={idCampo} aria-invalid={!!error} {...props} className={estiloEntrada(error)}>{children}</select>
    </Envoltura>
  );
}

// ─── Tarjeta y encabezado de página ──────────────────────────────────
export function Tarjeta({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <section className={`bg-superficie rounded-xl shadow-sm ring-1 ring-black/[0.04] p-8 ${className}`}>{children}</section>;
}

export function EncabezadoPagina({ titulo, descripcion, acciones }:
  { titulo: string; descripcion?: string; acciones?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-6 mb-8">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-texto">{titulo}</h1>
        {descripcion && <p className="text-secundario mt-1.5">{descripcion}</p>}
      </div>
      {acciones && <div className="flex gap-3">{acciones}</div>}
    </div>
  );
}

// ─── Etiqueta de estado (nombres exactos de tipos.ts) ────────────────
type Tono = 'exito' | 'alerta' | 'error' | 'info' | 'neutro';
const tonoPorEstado: Record<string, Tono> = {
  // Venta
  PendienteAbastecimiento: 'alerta', PendienteDespacho: 'alerta', ListaParaDespacho: 'info',
  Despachada: 'exito', Cancelada: 'error',
  // Cotización
  Emitida: 'info', Aceptada: 'exito', Rechazada: 'error', Vencida: 'error',
  // Guía de despacho
  EnPreparacion: 'neutro', SalidaConfirmada: 'info', Entregada: 'exito',
  // Compras y recepción
  Pendiente: 'alerta', Parcial: 'alerta', Completada: 'exito', Validada: 'exito',
  // Documento OCR y coincidencia
  'Procesado OCR': 'info', Validado: 'exito',
  Alta: 'exito', Media: 'alerta', Baja: 'error', 'No identificado': 'neutro',
  // Reserva de stock
  Activa: 'info', Liberada: 'neutro', Consumida: 'exito',
};
const estilosTono: Record<Tono, string> = {
  exito: 'bg-exito/10 text-exito ring-exito/20',
  alerta: 'bg-alerta/10 text-alerta ring-alerta/20',
  error: 'bg-error/10 text-error ring-error/20',
  info: 'bg-info/10 text-info ring-info/20',
  neutro: 'bg-secundario/10 text-secundario ring-secundario/20',
};
// Texto visible de cada estado ('PendienteAbastecimiento' → 'Pendiente de abastecimiento')
const textoEstado: Record<string, string> = {
  PendienteAbastecimiento: 'Pendiente de abastecimiento', PendienteDespacho: 'Pendiente de despacho',
  ListaParaDespacho: 'Lista para despacho', EnPreparacion: 'En preparación',
  SalidaConfirmada: 'Salida confirmada',
};
const legible = (estado: string) => textoEstado[estado] ?? estado;

export function EtiquetaEstado({ estado }: { estado: string }) {
  const tono = tonoPorEstado[estado] ?? 'neutro';
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-sm font-medium ring-1 ring-inset ${estilosTono[tono]}`}>
      <span className="size-1.5 rounded-full bg-current" aria-hidden />
      {legible(estado)}
    </span>
  );
}

// ─── Estado vacío ────────────────────────────────────────────────────
export function EstadoVacio({ icono, titulo, descripcion, accion }:
  { icono: ReactNode; titulo: string; descripcion?: string; accion?: ReactNode }) {
  return (
    <div className="mx-auto max-w-md py-16 px-6 flex flex-col items-center text-center">
      <span className="grid size-14 place-items-center rounded-2xl bg-primario-suave text-primario mb-5">{icono}</span>
      <h2 className="text-lg font-semibold text-texto">{titulo}</h2>
      {descripcion && <p className="text-secundario mt-2 leading-relaxed">{descripcion}</p>}
      {accion && <div className="mt-6">{accion}</div>}
    </div>
  );
}
