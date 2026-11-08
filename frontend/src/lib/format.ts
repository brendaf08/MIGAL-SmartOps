/** Formatos de presentación con la configuración regional es-PE (ver reglas de formatos del frontend). */
const LOCALE = 'es-PE';

export const formatNumber = (n: number, digits = 0) =>
  new Intl.NumberFormat(LOCALE, {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  }).format(n);

/** Importe en soles: `S/ 1,250.00`. */
export const formatCurrency = (n: number) => `S/\u00A0${formatNumber(n, 2)}`;

/** Fecha `dd/mm/aaaa`. Una fecha `aaaa-mm-dd` se interpreta en hora local para no retroceder un día. */
export const formatDate = (d: Date | string | number) =>
  new Intl.DateTimeFormat(LOCALE, { day: '2-digit', month: '2-digit', year: 'numeric' }).format(
    new Date(typeof d === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(d) ? `${d}T00:00:00` : d),
  );
