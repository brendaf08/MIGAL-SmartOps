/** Cliente registrado (CU-10, CU-09). Distinto de `comercial/types.ts`, que describe el cliente embebido en una cotización. */
export interface Cliente {
  id: string;
  tipoDocumento: 'DNI' | 'RUC';
  documento: string;
  nombre: string;
  telefono: string;
  correo: string;
  estado: 'Activo' | 'Inactivo';
}
