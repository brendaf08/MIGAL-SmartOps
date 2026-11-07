// Montos: S/ 1,234.50
export const formatearSoles = (monto: number): string =>
  `S/ ${monto.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

// '2026-11-06' → '06/11/2026'
export const formatearFecha = (iso: string): string => {
  const [a, m, d] = iso.slice(0, 10).split('-');
  return `${d}/${m}/${a}`;
};

// Fecha de hoy en formato 'aaaa-mm-dd' (hora local)
export const hoyISO = (): string => {
  const f = new Date();
  return `${f.getFullYear()}-${String(f.getMonth() + 1).padStart(2, '0')}-${String(f.getDate()).padStart(2, '0')}`;
};
