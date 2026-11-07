<!-- GENERADO con "npm run prompts". No editar a mano. -->
# Prompt para AI Studio · módulo productos (Brenda)

Copia TODO este archivo y pégalo como primer mensaje en un proyecto nuevo de Google AI Studio.

Eres un desarrollador frontend. Construye pantallas para MIGAL SmartOps, el sistema de gestión de una ferretería en Perú.

STACK OBLIGATORIO (no uses otras librerías)
- React + TypeScript, React Router (react-router-dom), Tailwind CSS y lucide-react para iconos.
- Sin backend: usa solo datos de prueba locales.
- Diseño para escritorio y tablet (desde 768 px). No diseñes para móvil.

ARCHIVOS COMPARTIDOS
- Crea EXACTAMENTE los archivos de src/shared/ y src/index.css que pego abajo. NO los modifiques nunca.
- Si Tailwind se carga por CDN, configura en tailwind.config los colores de src/index.css (primario #15803D, primario-oscuro #166534, primario-suave #DCFCE7, lateral #0B3B24, secundario #64748B, tenue #94A3B8, fondo #F8FAF9, superficie #FFFFFF, borde #EEF2EF, borde-campo #858F9C, texto #0F1A14, exito #15803D, info #1D4ED8, alerta #B45309, error #B91C1C) y las fuentes Inter (sans) y Plus Jakarta Sans (display).
- Usa solo esas clases de color (bg-primario, text-error, border-borde...). Nunca colores de Tailwind como green-600 o blue-600.

KIT VISUAL (obligatorio, para que todas las pantallas se vean iguales)
- Usa los componentes de src/shared/components/ui.tsx: EncabezadoPagina (título y botones de cada pantalla), Tarjeta (contenedores), Boton (variantes primario, secundario, peligro, fantasma), Campo y Selector (formularios con etiqueta y error) y EtiquetaEstado (estados).
- Cada pantalla empieza con <EncabezadoPagina titulo=... descripcion=... acciones=... /> y su contenido va en <Tarjeta>.
- Tablas: encabezado con bg-fondo text-secundario text-sm font-semibold, filas con border-b border-borde y hover:bg-fondo, celdas con px-4 py-3, montos alineados a la derecha.
- Estilo profesional y sobrio: bordes redondeados (rounded-xl en tarjetas, rounded-lg en botones), sombras suaves, mucho espacio en blanco, iconos de lucide-react de 18 a 20 px junto a las acciones.
- No dibujes menú lateral ni barra superior: ya existen en el sistema.
- Estados vacíos con <EstadoVacio icono titulo descripcion accion />.

PRINCIPIOS DE DISEÑO (estética SaaS B2B premium, nada de plantilla genérica)
- Espaciado generoso y consistente: gap-6, p-8, space-y-6. Nada amontonado.
- Capas: el lienzo es bg-fondo; tarjetas y contenedores en bg-superficie. Separa con sombras suaves (shadow-sm, ring-1 ring-black/[0.04]) en vez de bordes; si usas bordes, solo border-borde.
- Jerarquía de texto estricta: títulos text-texto, descripciones text-secundario, metadatos no esenciales text-tenue. Nunca negro puro.
- Iconos solo de lucide-react, tamaño 18 a 20 px (el trazo 1.5 ya está configurado globalmente).
- Botones rounded-lg con hover sutil (ya resuelto en Boton). Nunca colores sólidos brillantes para resaltar selecciones: usa bg-primario-suave o fondos translúcidos.
- Los campos de formulario siempre con borde visible (ya resuelto en Campo y Selector) para que se distingan.
- Crea src/App.tsx con el App de prueba que pego abajo, tal cual.

DÓNDE VA TU CÓDIGO
- Todo tu código va dentro de src/modules/productos/, con subcarpetas pages/, components/ y data/.
- src/modules/productos/index.tsx exporta: export const rutas: RutaModulo[] (tipo de src/shared/tipos.ts), con un elemento por pantalla (codigo, path, titulo, elemento, enMenu). Usa EXACTAMENTE los códigos, paths y enMenu que indico abajo.
- Cada pantalla es un archivo en pages/ con nombre código + PascalCase (ej. pages/P12AlertasStock.tsx) y export default.
- Importa solo desde src/shared/ con rutas relativas (../../shared/... o ../../../shared/...). Nunca importes de otro módulo. Para ir a otra pantalla usa su ruta con Link o useNavigate.
- No crees routers dentro del módulo: solo usa Link, useNavigate y useParams.

DATOS Y REGLAS
- Usa exactamente los tipos y nombres de campos de src/shared/tipos.ts, en snake_case. No traduzcas ni inventes campos.
- Lee los datos de src/shared/data/semillas.ts. Los datos nuevos del módulo se guardan en estado (useState) o en src/modules/productos/data/.
- El usuario y el rol se leen con useAuth() de src/shared/auth/AuthContext.tsx.
- Montos con formatearSoles() y fechas con formatearFecha() de src/shared/formato.ts (S/ y dd/mm/aaaa).
- El stock está por ubicación en inventario (Tienda principal, Almacén 1, Almacén 2); no existe un campo único de stock.
- Reglas visibles: nunca stock negativo; el descuento no puede superar el subtotal; el cliente es obligatorio en cotizaciones y ventas. Muestra mensajes de error claros y deshabilita el botón cuando no se cumpla.
- Muestra los estados con sus nombres exactos de tipos.ts, como etiquetas de color (exito, alerta, error, secundario).

ACCESIBILIDAD Y ESTILO
- Fuente Inter, texto base 16 px, áreas de clic de mínimo 44×44 px (min-h-11), contraste WCAG AA.
- Etiquetas visibles en todos los campos. Confirmación antes de acciones destructivas.
- Todo el texto de la interfaz en español.

Si algo no está especificado, elige la opción más simple y no agregues librerías.

## Archivos compartidos (créalos tal cual)

### src/index.css

```css
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Plus+Jakarta+Sans:wght@600;700;800&display=swap');
@import "tailwindcss";

/* Paleta MIGAL · verde profesional (contraste WCAG AA verificado sobre blanco) */
@theme {
  --color-primario: #15803D;        /* botones, enlaces, foco */
  --color-primario-oscuro: #166534; /* hover de botones, títulos de marca */
  --color-primario-suave: #DCFCE7;  /* fondos de selección */
  --color-lateral: #0B3B24;         /* menú lateral */
  --color-secundario: #64748B;      /* descripciones y texto secundario (slate-500) */
  --color-tenue: #94A3B8;           /* solo metadatos no esenciales (slate-400) */
  --color-fondo: #F8FAF9;           /* lienzo: gris muy sutil */
  --color-superficie: #FFFFFF;      /* tarjetas, modales */
  --color-borde: #EEF2EF;           /* bordes extremadamente sutiles */
  --color-borde-campo: #858F9C;     /* borde de inputs: contraste 3:1 (WCAG 1.4.11) */
  --color-texto: #0F1A14;           /* títulos casi negro, nunca #000 */
  --color-exito: #15803D;
  --color-info: #1D4ED8;
  --color-alerta: #B45309;
  --color-error: #B91C1C;
  --font-sans: "Inter", system-ui, sans-serif;
  --font-display: "Plus Jakarta Sans", "Inter", system-ui, sans-serif;
}

html { font-size: 16px; }
body { @apply bg-fondo text-texto font-sans antialiased; }
h1, h2, h3 { @apply font-display; }

/* Iconos lucide con trazo constante de 1.5 */
svg.lucide { stroke-width: 1.5px; }

/* Barra de desplazamiento discreta en el menú lateral */
.menu-lateral { scrollbar-width: thin; scrollbar-color: rgb(255 255 255 / 0.2) transparent; }

@media print {
  .no-imprimir { display: none !important; }
  body { background: white; }
}
```

### src/shared/tipos.ts

```ts
import type { ReactNode } from 'react';

// ─── Roles y pantallas ───────────────────────────────────────────────
export type Rol = 'administrador' | 'vendedor' | 'almacenero' | 'reponedor';

export type CodigoPantalla =
  | 'P01' | 'P02' | 'P03' | 'P04' | 'P05' | 'P06' | 'P07' | 'P08' | 'P09'
  | 'P10' | 'P11' | 'P12' | 'P13' | 'P14' | 'P15' | 'P16' | 'P17' | 'P18'
  | 'P19' | 'P20' | 'P21' | 'P22' | 'P23' | 'P24' | 'P25' | 'P26' | 'P27';

// Contrato de cada módulo: src/modules/<modulo>/index.tsx exporta `rutas: RutaModulo[]`
export interface RutaModulo {
  codigo: CodigoPantalla;
  path: string;
  titulo: string;
  elemento: ReactNode;
  enMenu: boolean;
}

// ─── Estados y catálogos (nombres exactos) ───────────────────────────
// Venta: la venta solo RESERVA stock. El stock baja (salida de Kardex) al confirmar el despacho.
// PendienteAbastecimiento: falta stock · PendienteDespacho: reservado completo, sin guía
// ListaParaDespacho: guía emitida · Despachada: salida confirmada · Cancelada: con motivo
export type EstadoVenta = 'PendienteAbastecimiento' | 'PendienteDespacho' | 'ListaParaDespacho'
  | 'Despachada' | 'Cancelada';
export type EstadoCotizacion = 'Emitida' | 'Aceptada' | 'Rechazada' | 'Vencida';
export type EstadoOrdenCompra = 'Pendiente' | 'Parcial' | 'Completada';
export type EstadoRecepcion = 'Pendiente' | 'Validada';
export type EstadoDocumento = 'Pendiente' | 'Procesado OCR' | 'Validado';
// Guía de despacho. Al pasar a SalidaConfirmada se registran las salidas de Kardex (origen Despacho).
export type EstadoDespacho = 'EnPreparacion' | 'Emitida' | 'SalidaConfirmada' | 'Entregada';
export type EstadoReserva = 'Activa' | 'Liberada' | 'Consumida';
export type TipoOperacion = 'Entrada' | 'Salida';
// Una merma se registra como Ajuste (Salida) con motivo «Merma».
export type OrigenMovimiento = 'Despacho' | 'Recepcion' | 'Ajuste' | 'Traslado';
export type NivelCoincidencia = 'Alta' | 'Media' | 'Baja' | 'No identificado';

// ─── Entidades: diccionario de datos del informe PC2 (Tablas 16 y 17) ─
// Fechas: texto 'aaaa-mm-dd' (o 'aaaa-mm-ddThh:mm' si lleva hora).
// En pantalla se muestran con formatearFecha(). password_hash no existe en el frontend.
// Campos con ? son opcionales en el diccionario.

export interface Usuario {
  id_usuario: string; nombres: string; apellidos: string; username: string;
  rol: Rol; estado_activo: boolean; fecha_registro: string; fecha_actualizacion?: string;
}

export interface Cliente {
  id_cliente: string; tipo_documento?: 'DNI' | 'RUC'; numero_documento?: string;
  nombre_razon_social: string; telefono_contacto?: string; direccion_referencia?: string;
  estado_activo: boolean; fecha_registro: string;
}

export interface Almacen {
  id_almacen: string; nombre: string; tipo_ubicacion: string;
  direccion_referencia?: string; estado_activo: boolean;
}

export interface Producto {
  id_producto: string; sku: string; nombre_oficial: string; descripcion?: string;
  unidad_medida: string; precio_referencial: number; stock_minimo: number;
  estado_activo: boolean; categoria_producto: string; marca?: string; presentacion?: string;
  fecha_registro: string; fecha_actualizacion?: string;
}

// Tabla 17 · Producto + Almacen. cantidad_libre = cantidad_fisica - cantidad_reservada
export interface Inventario {
  id_inventario: string; id_producto: string; id_almacen: string;
  cantidad_fisica: number; cantidad_reservada: number; cantidad_libre: number;
}

export interface Proveedor {
  id_proveedor: string; ruc: string; razon_social: string; contacto: string;
  condiciones?: string; estado_activo: boolean; fecha_registro: string;
}

// Tabla 17 · Proveedor + Producto
export interface CatalogoProveedor {
  id_proveedor: string; id_producto: string; precio_compra: number;
  plazo_entrega_referencial_dias?: number; fecha_actualizacion_precio?: string;
}

// ─── Ventas (Pinedo) ─────────────────────────────────────────────────
export interface Venta {
  id_venta: string; id_usuario_responsable: string; id_cotizacion: string | null;
  id_cliente: string; // obligatorio por decisión del equipo
  // id_cotizacion es null en una venta directa (decisión del equipo)
  fecha_venta: string; descuento: number; total_venta: number; estado: EstadoVenta;
  fecha_cancelacion?: string; motivo_cancelacion?: string;
}

// Tabla 17 · Venta + Producto
export interface DetalleVenta {
  id_detalle_venta: string; id_venta: string; id_producto: string;
  cantidad_vendida: number; precio_unitario: number; subtotal_detalle: number;
  cantidad_reservada_actual: number; cantidad_pendiente_abastecimiento: number;
}

// Tabla 17 · Detalle_Venta + Inventario
export interface ReservaStock {
  id_reserva: string; id_detalle_venta: string; id_inventario: string;
  cantidad_reservada: number; estado: EstadoReserva; fecha_reserva: string;
  fecha_cierre?: string; id_usuario_responsable: string;
}

// ─── Cotizaciones (Alexander) ────────────────────────────────────────
export interface Cotizacion {
  id_cotizacion: string; id_cliente: string; id_usuario_responsable: string;
  fecha_emision: string; fecha_vencimiento: string; condiciones_comerciales?: string;
  descuento: number; estado: EstadoCotizacion; total_cotizacion: number;
  fecha_aceptacion_cliente?: string; fecha_validacion_migal?: string;
}

// Tabla 17 · Cotizacion + Producto
export interface DetalleCotizacion {
  id_cotizacion: string; id_producto: string;
  cantidad_cotizada: number; precio_unitario: number; subtotal_detalle: number;
}

// ─── Despacho (Nuñez) ────────────────────────────────────────────────
export interface GuiaDespacho {
  id_guia: string; id_venta: string; fecha_despacho?: string; direccion_entrega: string;
  estado: EstadoDespacho; fecha_emision: string;
  fecha_salida_real?: string; id_usuario_responsable?: string;
}

// ─── Kardex (Nuñez) ──────────────────────────────────────────────────
export interface MovimientoKardex {
  id_movimiento: string; id_inventario: string; fecha_hora: string;
  tipo_operacion: TipoOperacion; cantidad: number; motivo?: string;
  id_usuario_responsable: string; origen_movimiento: OrigenMovimiento;
  id_guia?: string; id_recepcion?: string; referencia_traslado?: string;
}

// ─── Compras (Brenda) ────────────────────────────────────────────────
export interface OrdenCompra {
  id_orden: string; id_proveedor: string; id_usuario_responsable: string;
  fecha_emision: string; estado: EstadoOrdenCompra;
  fecha_entrega_estimada?: string; observaciones?: string; modalidad_pago?: string;
}

// Tabla 17 · Orden_Compra + Producto
export interface DetalleCompra {
  id_orden: string; id_producto: string; cantidad_comprada: number;
  costo_unitario: number; subtotal_detalle: number; cantidad_pendiente_recepcion: number;
}

// ─── Recepción (Alexander) ───────────────────────────────────────────
export interface OrdenRecepcion {
  id_recepcion: string; id_orden: string; id_usuario_responsable: string;
  fecha_recepcion: string; estado: EstadoRecepcion;
  fecha_validacion?: string; observaciones?: string;
}

// Tabla 17 · Orden_Recepcion + Producto (+ almacén destino)
export interface DetalleRecepcion {
  id_recepcion: string; id_producto: string; id_almacen_destino: string; cantidad_recibida: number;
}

// ─── OCR (Alexander) ─────────────────────────────────────────────────
export interface DocumentoDigital {
  id_documento: string; ruta_archivo: string; fecha_subida: string;
  estado_procesamiento: EstadoDocumento;
  tipo_documento: 'SolicitudCliente' | 'DocumentoRecepcion';
  id_usuario: string; id_cotizacion?: string; id_recepcion?: string;
}

export interface DetalleExtraido {
  id_detalle_extraido: string; id_documento: string; texto_crudo: string;
  cantidad_leida?: number; id_producto?: string; texto_corregido?: string;
}

// Lo que muestra la revisión del OCR (P24). nivel_coincidencia se CALCULA, no se guarda.
export interface DetalleConCoincidencia extends DetalleExtraido {
  nivel_coincidencia: NivelCoincidencia;
}
```

### src/shared/formato.ts

```ts
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
```

### src/shared/data/semillas.ts

```ts
import type {
  Usuario, Cliente, Producto, Almacen, Inventario, Proveedor, CatalogoProveedor,
  Venta, DetalleVenta, Cotizacion, DetalleCotizacion, OrdenCompra, DetalleCompra,
} from '../tipos';

// Contraseña de los 4 usuarios de prueba: migal123
export const usuarios: Usuario[] = [
  { id_usuario: 'USR-001', nombres: 'Ana', apellidos: 'Torres', username: 'admin', rol: 'administrador', estado_activo: true, fecha_registro: '2026-01-10' },
  { id_usuario: 'USR-002', nombres: 'Luis', apellidos: 'Quispe', username: 'vendedor', rol: 'vendedor', estado_activo: true, fecha_registro: '2026-02-03' },
  { id_usuario: 'USR-003', nombres: 'Rosa', apellidos: 'Huamán', username: 'almacenero', rol: 'almacenero', estado_activo: true, fecha_registro: '2026-02-15' },
  { id_usuario: 'USR-004', nombres: 'Carlos', apellidos: 'Ramos', username: 'reponedor', rol: 'reponedor', estado_activo: true, fecha_registro: '2026-03-01' },
];

export const almacenes: Almacen[] = [
  { id_almacen: 'ALM-001', nombre: 'Tienda principal', tipo_ubicacion: 'tienda', direccion_referencia: 'Av. Principal 100', estado_activo: true },
  { id_almacen: 'ALM-002', nombre: 'Almacén 1', tipo_ubicacion: 'almacen', estado_activo: true },
  { id_almacen: 'ALM-003', nombre: 'Almacén 2', tipo_ubicacion: 'almacen', estado_activo: true },
];

export const productos: Producto[] = [
  { id_producto: 'PRD-001', sku: 'CEM-001', nombre_oficial: 'Cemento Portland tipo I', descripcion: 'Cemento de uso general', unidad_medida: 'bolsa', precio_referencial: 32.5, stock_minimo: 20, estado_activo: true, categoria_producto: 'Construcción', marca: 'Sol', presentacion: 'Bolsa de 42.5 kg', fecha_registro: '2026-01-15' },
  { id_producto: 'PRD-002', sku: 'FIE-012', nombre_oficial: 'Fierro corrugado 1/2"', descripcion: 'Varilla de construcción', unidad_medida: 'varilla', precio_referencial: 38.9, stock_minimo: 50, estado_activo: true, categoria_producto: 'Construcción', marca: 'Aceros Arequipa', presentacion: 'Varilla de 9 m', fecha_registro: '2026-01-15' },
  { id_producto: 'PRD-003', sku: 'PIN-004', nombre_oficial: 'Pintura látex blanco', descripcion: 'Pintura para interiores', unidad_medida: 'galón', precio_referencial: 55, stock_minimo: 10, estado_activo: true, categoria_producto: 'Pinturas', marca: 'CPP', presentacion: 'Galón de 4 L', fecha_registro: '2026-01-20' },
  { id_producto: 'PRD-004', sku: 'TUB-020', nombre_oficial: 'Tubo PVC 1/2" agua', descripcion: 'Tubo para agua fría', unidad_medida: 'unidad', precio_referencial: 8.5, stock_minimo: 30, estado_activo: true, categoria_producto: 'Gasfitería', marca: 'Pavco', presentacion: 'Tubo de 5 m', fecha_registro: '2026-02-01' },
  { id_producto: 'PRD-005', sku: 'CLA-003', nombre_oficial: 'Clavo de 3"', descripcion: 'Clavo para madera', unidad_medida: 'kg', precio_referencial: 6.8, stock_minimo: 15, estado_activo: true, categoria_producto: 'Ferretería', marca: 'Genérica', presentacion: 'Kilo', fecha_registro: '2026-02-01' },
];

// cantidad_libre = cantidad_fisica - cantidad_reservada
// PRD-003 y PRD-005 quedan bajo su stock mínimo (para probar alertas).
export const inventario: Inventario[] = [
  { id_inventario: 'INV-001', id_producto: 'PRD-001', id_almacen: 'ALM-001', cantidad_fisica: 30, cantidad_reservada: 5, cantidad_libre: 25 },
  { id_inventario: 'INV-002', id_producto: 'PRD-001', id_almacen: 'ALM-002', cantidad_fisica: 120, cantidad_reservada: 0, cantidad_libre: 120 },
  { id_inventario: 'INV-003', id_producto: 'PRD-002', id_almacen: 'ALM-001', cantidad_fisica: 40, cantidad_reservada: 10, cantidad_libre: 30 },
  { id_inventario: 'INV-004', id_producto: 'PRD-002', id_almacen: 'ALM-003', cantidad_fisica: 200, cantidad_reservada: 0, cantidad_libre: 200 },
  { id_inventario: 'INV-005', id_producto: 'PRD-003', id_almacen: 'ALM-001', cantidad_fisica: 4, cantidad_reservada: 1, cantidad_libre: 3 },
  { id_inventario: 'INV-006', id_producto: 'PRD-003', id_almacen: 'ALM-002', cantidad_fisica: 2, cantidad_reservada: 0, cantidad_libre: 2 },
  { id_inventario: 'INV-007', id_producto: 'PRD-004', id_almacen: 'ALM-001', cantidad_fisica: 25, cantidad_reservada: 0, cantidad_libre: 25 },
  { id_inventario: 'INV-008', id_producto: 'PRD-004', id_almacen: 'ALM-003', cantidad_fisica: 60, cantidad_reservada: 0, cantidad_libre: 60 },
  { id_inventario: 'INV-009', id_producto: 'PRD-005', id_almacen: 'ALM-001', cantidad_fisica: 8, cantidad_reservada: 0, cantidad_libre: 8 },
];

export const clientes: Cliente[] = [
  { id_cliente: 'CLI-001', tipo_documento: 'DNI', numero_documento: '45678912', nombre_razon_social: 'Juan Pérez Quispe', telefono_contacto: '987654321', direccion_referencia: 'Av. Los Olivos 123', estado_activo: true, fecha_registro: '2026-03-05' },
  { id_cliente: 'CLI-002', tipo_documento: 'RUC', numero_documento: '20600000001', nombre_razon_social: 'Constructora Los Andes S.A.C.', telefono_contacto: '014567890', direccion_referencia: 'Jr. Lima 456', estado_activo: true, fecha_registro: '2026-03-10' },
  { id_cliente: 'CLI-003', tipo_documento: 'DNI', numero_documento: '70123456', nombre_razon_social: 'María Flores Rojas', telefono_contacto: '912345678', direccion_referencia: 'Calle Las Flores 789', estado_activo: true, fecha_registro: '2026-04-02' },
];

export const proveedores: Proveedor[] = [
  { id_proveedor: 'PRV-001', ruc: '20600000002', razon_social: 'Distribuidora Ferretera del Sur S.A.C.', contacto: 'Marta Díaz – 999111222', condiciones: 'Crédito a 30 días', estado_activo: true, fecha_registro: '2026-01-12' },
  { id_proveedor: 'PRV-002', ruc: '20600000003', razon_social: 'Aceros y Más E.I.R.L.', contacto: 'Jorge Silva – 999333444', condiciones: 'Contado', estado_activo: true, fecha_registro: '2026-01-18' },
];

export const catalogoProveedor: CatalogoProveedor[] = [
  { id_proveedor: 'PRV-001', id_producto: 'PRD-001', precio_compra: 27, plazo_entrega_referencial_dias: 3 },
  { id_proveedor: 'PRV-001', id_producto: 'PRD-003', precio_compra: 45, plazo_entrega_referencial_dias: 5 },
  { id_proveedor: 'PRV-002', id_producto: 'PRD-002', precio_compra: 35, plazo_entrega_referencial_dias: 7 },
];

// ─── Semillas que cruzan módulos ─────────────────────────────────────
// Cotización: la lee Pinedo en P20 (venta desde cotización Aceptada)
export const cotizaciones: Cotizacion[] = [
  { id_cotizacion: 'COT-001', id_cliente: 'CLI-002', id_usuario_responsable: 'USR-002', fecha_emision: '2026-11-02', fecha_vencimiento: '2026-11-16', condiciones_comerciales: 'Entrega en obra', descuento: 61, estado: 'Aceptada', total_cotizacion: 1300, fecha_aceptacion_cliente: '2026-11-03' },
  { id_cotizacion: 'COT-002', id_cliente: 'CLI-001', id_usuario_responsable: 'USR-002', fecha_emision: '2026-11-04', fecha_vencimiento: '2026-11-18', descuento: 0, estado: 'Emitida', total_cotizacion: 110 },
  { id_cotizacion: 'COT-003', id_cliente: 'CLI-003', id_usuario_responsable: 'USR-002', fecha_emision: '2026-10-01', fecha_vencimiento: '2026-10-15', descuento: 0, estado: 'Vencida', total_cotizacion: 65 },
];
export const detallesCotizacion: DetalleCotizacion[] = [
  { id_cotizacion: 'COT-001', id_producto: 'PRD-001', cantidad_cotizada: 20, precio_unitario: 32.5, subtotal_detalle: 650 },
  { id_cotizacion: 'COT-001', id_producto: 'PRD-002', cantidad_cotizada: 18, precio_unitario: 39.5, subtotal_detalle: 711 },
  { id_cotizacion: 'COT-002', id_producto: 'PRD-003', cantidad_cotizada: 2, precio_unitario: 55, subtotal_detalle: 110 },
  { id_cotizacion: 'COT-003', id_producto: 'PRD-001', cantidad_cotizada: 2, precio_unitario: 32.5, subtotal_detalle: 65 },
];

// Venta: la lee Nuñez en P15 (ventas PendienteDespacho y ListaParaDespacho)
export const ventas: Venta[] = [
  { id_venta: 'VEN-001', id_usuario_responsable: 'USR-002', id_cotizacion: null, id_cliente: 'CLI-001', fecha_venta: '2026-11-03', descuento: 0, total_venta: 77.8, estado: 'Despachada' },
  { id_venta: 'VEN-002', id_usuario_responsable: 'USR-002', id_cotizacion: 'COT-001', id_cliente: 'CLI-002', fecha_venta: '2026-11-04', descuento: 61, total_venta: 1300, estado: 'PendienteDespacho' },
  { id_venta: 'VEN-003', id_usuario_responsable: 'USR-002', id_cotizacion: null, id_cliente: 'CLI-003', fecha_venta: '2026-11-05', descuento: 0, total_venta: 550, estado: 'PendienteAbastecimiento' },
  { id_venta: 'VEN-004', id_usuario_responsable: 'USR-002', id_cotizacion: null, id_cliente: 'CLI-001', fecha_venta: '2026-11-05', descuento: 5, total_venta: 80, estado: 'PendienteDespacho' },
];
export const detallesVenta: DetalleVenta[] = [
  { id_detalle_venta: 'DVE-001', id_venta: 'VEN-001', id_producto: 'PRD-002', cantidad_vendida: 2, precio_unitario: 38.9, subtotal_detalle: 77.8, cantidad_reservada_actual: 0, cantidad_pendiente_abastecimiento: 0 },
  { id_detalle_venta: 'DVE-002', id_venta: 'VEN-002', id_producto: 'PRD-001', cantidad_vendida: 20, precio_unitario: 32.5, subtotal_detalle: 650, cantidad_reservada_actual: 5, cantidad_pendiente_abastecimiento: 0 },
  { id_detalle_venta: 'DVE-003', id_venta: 'VEN-002', id_producto: 'PRD-002', cantidad_vendida: 18, precio_unitario: 39.5, subtotal_detalle: 711, cantidad_reservada_actual: 10, cantidad_pendiente_abastecimiento: 0 },
  { id_detalle_venta: 'DVE-004', id_venta: 'VEN-003', id_producto: 'PRD-003', cantidad_vendida: 10, precio_unitario: 55, subtotal_detalle: 550, cantidad_reservada_actual: 1, cantidad_pendiente_abastecimiento: 7 },
  { id_detalle_venta: 'DVE-005', id_venta: 'VEN-004', id_producto: 'PRD-004', cantidad_vendida: 10, precio_unitario: 8.5, subtotal_detalle: 85, cantidad_reservada_actual: 0, cantidad_pendiente_abastecimiento: 0 },
];

// Orden de compra: la lee Alexander en P27 (recepción asociada a una orden)
export const ordenesCompra: OrdenCompra[] = [
  { id_orden: 'OC-001', id_proveedor: 'PRV-001', id_usuario_responsable: 'USR-004', fecha_emision: '2026-11-01', estado: 'Pendiente', fecha_entrega_estimada: '2026-11-08', modalidad_pago: 'Crédito 30 días' },
  { id_orden: 'OC-002', id_proveedor: 'PRV-002', id_usuario_responsable: 'USR-004', fecha_emision: '2026-10-28', estado: 'Parcial', fecha_entrega_estimada: '2026-11-04', modalidad_pago: 'Contado' },
  { id_orden: 'OC-003', id_proveedor: 'PRV-001', id_usuario_responsable: 'USR-004', fecha_emision: '2026-10-15', estado: 'Completada' },
];
export const detallesCompra: DetalleCompra[] = [
  { id_orden: 'OC-001', id_producto: 'PRD-003', cantidad_comprada: 20, costo_unitario: 45, subtotal_detalle: 900, cantidad_pendiente_recepcion: 20 },
  { id_orden: 'OC-002', id_producto: 'PRD-002', cantidad_comprada: 100, costo_unitario: 35, subtotal_detalle: 3500, cantidad_pendiente_recepcion: 40 },
  { id_orden: 'OC-003', id_producto: 'PRD-005', cantidad_comprada: 100, costo_unitario: 6, subtotal_detalle: 600, cantidad_pendiente_recepcion: 0 },
];
```

### src/shared/auth/AuthContext.tsx

```tsx
import { createContext, useContext, useState, type ReactNode } from 'react';
import type { Rol, Usuario } from '../tipos';
import { usuarios } from '../data/semillas';

const CLAVE_PRUEBA = 'migal123'; // misma contraseña para los 4 usuarios de prueba

interface Sesion {
  usuario: Usuario | null;
  rol: Rol | null;
  iniciarSesion: (username: string, password: string) => boolean;
  cerrarSesion: () => void;
}

const AuthContext = createContext<Sesion | null>(null);

export function AuthProvider({ children, usuarioInicial = null }:
  { children: ReactNode; usuarioInicial?: Usuario | null }) {
  const [usuario, setUsuario] = useState<Usuario | null>(usuarioInicial);

  const iniciarSesion = (username: string, password: string): boolean => {
    const encontrado = usuarios.find((u) => u.username === username && u.estado_activo);
    if (!encontrado || password !== CLAVE_PRUEBA) return false;
    setUsuario(encontrado);
    return true;
  };

  return (
    <AuthContext.Provider value={{ usuario, rol: usuario?.rol ?? null, iniciarSesion, cerrarSesion: () => setUsuario(null) }}>
      {children}
    </AuthContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): Sesion {
  const sesion = useContext(AuthContext);
  if (!sesion) throw new Error('useAuth debe usarse dentro de AuthProvider');
  return sesion;
}
```

### src/shared/auth/permisos.ts

```ts
import type { CodigoPantalla, Rol } from '../tipos';

// El administrador ve todo. P01 (login) es pública. P04 es un modal dentro de P02.
export const permisos: Record<Exclude<Rol, 'administrador'>, CodigoPantalla[]> = {
  vendedor:   ['P05', 'P11', 'P17', 'P18', 'P19', 'P20', 'P21', 'P22', 'P23', 'P24'],
  almacenero: ['P05', 'P08', 'P09', 'P10', 'P11', 'P12', 'P13', 'P14', 'P15', 'P16', 'P23', 'P24', 'P27'],
  reponedor:  ['P05', 'P06', 'P07', 'P12', 'P25', 'P26'],
};

export function puedeVer(rol: Rol | null, codigo: CodigoPantalla): boolean {
  if (codigo === 'P01') return true;
  if (!rol) return false;
  if (rol === 'administrador') return true;
  return permisos[rol].includes(codigo);
}
```

### src/shared/components/ui.tsx

```tsx
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
```

### src/shared/components/Logo.tsx

```tsx
// Logo de la marca. Coloca el archivo en public/logo.png (fondo transparente).
// Si no existe, se muestra un isotipo de respaldo.
import { useState } from 'react';
import { Wrench } from 'lucide-react';

export default function Logo({ claro = false, compacto = false }: { claro?: boolean; compacto?: boolean }) {
  const [sinImagen, setSinImagen] = useState(false);
  return (
    <div className="flex items-center gap-3">
      {sinImagen ? (
        <span className={`grid size-10 place-items-center rounded-xl ${claro ? 'bg-white/10 text-white' : 'bg-primario text-white'}`}>
          <Wrench size={22} aria-hidden />
        </span>
      ) : (
        <img src="/logo.png" alt="Logo" className="h-10 w-auto" onError={() => setSinImagen(true)} />
      )}
      {!compacto && (
        <div className="leading-tight">
          <p className={`font-display text-lg font-extrabold tracking-tight ${claro ? 'text-white' : 'text-primario-oscuro'}`}>MIGAL</p>
          <p className={`text-xs font-medium uppercase tracking-widest ${claro ? 'text-white/60' : 'text-secundario'}`}>SmartOps</p>
        </div>
      )}
    </div>
  );
}
```

### src/shared/components/EnConstruccion.tsx

```tsx
import { Hammer } from 'lucide-react';
import { EncabezadoPagina, EstadoVacio, Tarjeta } from './ui';

export default function EnConstruccion({ codigo, titulo }: { codigo: string; titulo: string }) {
  return (
    <>
      <EncabezadoPagina titulo={titulo} descripcion={`Pantalla ${codigo}`} />
      <Tarjeta>
        <EstadoVacio icono={<Hammer size={26} aria-hidden />} titulo="En construcción"
          descripcion="Esta pantalla se está diseñando. Muy pronto estará disponible." />
      </Tarjeta>
    </>
  );
}
```

### src/App.tsx (App de prueba)

```tsx
import { HashRouter, Routes, Route, Link } from 'react-router-dom';
import { AuthProvider } from './shared/auth/AuthContext';
import { usuarios } from './shared/data/semillas';
import { rutas } from './modules/productos';

// SOLO PARA PROBAR EN AI STUDIO. Para probar otro rol cambia el índice:
// usuarios[0] admin · [1] vendedor · [2] almacenero · [3] reponedor
export default function App() {
  return (
    <AuthProvider usuarioInicial={usuarios[0]}>
      <HashRouter>
        <nav className="flex flex-wrap gap-2 p-4 bg-superficie border-b">
          {rutas.map((r) => (
            <Link key={r.codigo} to={r.path.replace(/:id/g, '1')} className="min-h-11 px-4 py-2 rounded text-primario">
              {r.codigo} {r.titulo}
            </Link>
          ))}
        </nav>
        <main className="p-6">
          <Routes>
            {rutas.map((r) => <Route key={r.codigo} path={r.path} element={r.elemento} />)}
          </Routes>
        </main>
      </HashRouter>
    </AuthProvider>
  );
}
```

## Pantallas a generar (módulo productos)

- P08 Productos: lista · /productos · enMenu true · CU-03. Tabla con sku, nombre_oficial, categoria_producto, marca, precio_referencial y stock libre total (suma de cantidad_libre en inventario); etiqueta «Stock crítico» si está bajo stock_minimo; búsqueda y filtro por categoría; cada fila lleva a /productos/<id_producto>.
- P09 Producto: formulario · /productos/nuevo · enMenu false · CU-03. Todos los campos de Producto; sku único, precio_referencial mayor a 0, stock_minimo de 0 o más.
- P10 Producto: ficha · /productos/:id · enMenu false · CU-03. Datos del producto y tabla de stock por ubicación (cantidad_fisica, cantidad_reservada, cantidad_libre).
- P11 Disponibilidad · /disponibilidad · enMenu true · CU-04. Buscador de producto; muestra las 3 ubicaciones con sus cantidades, resaltando dónde hay stock libre.
- P25 Órdenes de compra: lista · /compras · enMenu true · CU-12. Lista de ordenesCompra con estados Pendiente, Parcial, Completada; filtro por estado y proveedor (OrdenCompra y DetalleCompra de semillas).
- P26 Orden de compra: nueva · /compras/nueva · enMenu false · CU-12. Proveedor activo, productos (sugiere precio_compra de catalogoProveedor), cantidad_comprada y costo_unitario; subtotal_detalle calculado; cantidades mayores a 0; estado inicial Pendiente; fecha_entrega_estimada y modalidad_pago opcionales.
