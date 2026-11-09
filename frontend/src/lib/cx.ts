/** Une nombres de clase descartando los valores falsos. */
export const cx = (...names: unknown[]) => names.filter(Boolean).join(' ');
