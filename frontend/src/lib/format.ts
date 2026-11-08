/** Formatos de presentación con la configuración regional es-PE (ver reglas de formatos del frontend). */
const LOCALE = 'es-PE';

export const formatNumber = (n: number, digits = 0) =>
  new Intl.NumberFormat(LOCALE, {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  }).format(n);

/** Importe en soles: `S/ 1,250.00`. */
export const formatCurrency = (n: number) => `S/\u00A0${formatNumber(n, 2)}`;

/** Fecha `dd/mm/aaaa`. */
export const formatDate = (d: Date | string | number) =>
  new Intl.DateTimeFormat(LOCALE, { day: '2-digit', month: '2-digit', year: 'numeric' }).format(
    new Date(d),
  );
