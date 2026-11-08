/**
 * Trazados de los íconos sobre una rejilla de 24 px (trazo de 1.75 px). Cada figura es un trazado SVG (`string`),
 * un círculo `{ c: [cx, cy, r] }` o un rectángulo `{ r: [x, y, ancho, alto, radio] }`.
 */
export type Shape =
  string | { c: [number, number, number] } | { r: [number, number, number, number, number] };

export const ICONS = {
  search: [{ c: [11, 11, 6.5] }, 'M20 20l-4.2-4.2'],
  plus: ['M12 5v14M5 12h14'],
  minus: ['M5 12h14'],
  close: ['M6 6l12 12M18 6L6 18'],
  check: ['M5 12.5l4.5 4.5L19 7.5'],
  'chevron-down': ['M6 9l6 6 6-6'],
  'chevron-up': ['M6 15l6-6 6 6'],
  'chevron-left': ['M15 6l-6 6 6 6'],
  'chevron-right': ['M9 6l6 6-6 6'],
  'chevrons-up-down': ['M8 9l4-4 4 4M8 15l4 4 4-4'],
  'arrow-right': ['M5 12h14M13 6l6 6-6 6'],
  'arrow-left': ['M19 12H5M11 6l-6 6 6 6'],
  'arrow-up': ['M12 19V5M6 11l6-6 6 6'],
  'arrow-down': ['M12 5v14M6 13l6 6 6-6'],
  edit: ['M4 20v-4L14.5 5.5a2.1 2.1 0 0 1 3 0l1 1a2.1 2.1 0 0 1 0 3L8 20z', 'M13 7l4 4'],
  trash: ['M4 7h16M10 11v6M14 11v6M6 7l1 12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1l1-12M9 7V4h6v3'],
  eye: ['M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z', { c: [12, 12, 3] }],
  download: ['M12 4v11M7 10l5 5 5-5M5 20h14'],
  upload: ['M12 16V5M7 10l5-5 5 5M5 20h14'],
  print: [
    'M7 9V4h10v5M7 17H5a1 1 0 0 1-1-1v-5a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v5a1 1 0 0 1-1 1h-2M7 14h10v6H7z',
  ],
  filter: ['M4 5h16l-6 7v6l-4 2v-8z'],
  calendar: [{ r: [4, 5, 16, 15, 2] }, 'M4 10h16M9 3v4M15 3v4'],
  user: [{ c: [12, 8, 4] }, 'M4 20a8 8 0 0 1 16 0'],
  users: [
    { c: [9, 8, 3.5] },
    'M3 19a6 6 0 0 1 12 0',
    { c: [17, 9, 2.5] },
    'M17 14a5 5 0 0 1 4.5 5',
  ],
  lock: [{ r: [5, 11, 14, 9, 2] }, 'M8 11V8a4 4 0 0 1 8 0v3'],
  logout: [
    'M14 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2v-2M10 12h10M17 9l3 3-3 3',
  ],
  menu: ['M4 7h16M4 12h16M4 17h16'],
  more: [{ c: [5, 12, 1.2] }, { c: [12, 12, 1.2] }, { c: [19, 12, 1.2] }],
  info: [{ c: [12, 12, 9] }, 'M12 11v5M12 8h.01'],
  'alert-triangle': ['M12 4.5L3 19.5h18z', 'M12 10v4M12 17h.01'],
  'alert-circle': [{ c: [12, 12, 9] }, 'M12 7.5v5M12 16h.01'],
  'check-circle': [{ c: [12, 12, 9] }, 'M8 12.5l2.7 2.7L16 9.5'],
  'x-circle': [{ c: [12, 12, 9] }, 'M9 9l6 6M15 9l-6 6'],
  'status-ok': [{ r: [4, 4, 16, 16, 3] }, 'M8.5 12.5l2.5 2.5 4.5-5'],
  'status-danger': ['M8.5 3h7L21 8.5v7L15.5 21h-7L3 15.5v-7z', 'M12 8v5M12 16h.01'],
  refresh: ['M21 4v5h-5M20 13a8 8 0 1 1-2.4-6.6L21 9'],
  clock: [{ c: [12, 12, 9] }, 'M12 7v5l3 2'],
  sliders: ['M4 7h9M17 7h3M4 17h3M11 17h9', { c: [15, 7, 2] }, { c: [9, 17, 2] }],
  send: ['M20 4L10.5 13.5M20 4l-6 16-3.5-6.5L4 10z'],
  sun: [
    { c: [12, 12, 4] },
    'M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M5.6 18.4L7 17M17 7l1.4-1.4',
  ],
  moon: ['M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z'],
  camera: [{ r: [3, 7, 18, 13, 2] }, 'M9 7l1.5-2h3L15 7', { c: [12, 13.5, 3.5] }],
  sparkles: [
    'M10 4l1.6 4.4L16 10l-4.4 1.6L10 16l-1.6-4.4L4 10l4.4-1.6z',
    'M18 14l.8 2.2L21 17l-2.2.8L18 20l-.8-2.2L15 17l2.2-.8z',
  ],
  bell: ['M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15z', 'M10 20a2 2 0 0 0 4 0'],
  dashboard: [
    { r: [4, 4, 7, 7, 1.5] },
    { r: [13, 4, 7, 4, 1.5] },
    { r: [13, 11, 7, 9, 1.5] },
    { r: [4, 14, 7, 6, 1.5] },
  ],
  help: [{ c: [12, 12, 9] }, 'M9.5 9.5a2.5 2.5 0 1 1 3.5 2.3c-.7.4-1 .9-1 1.7M12 17h.01'],
  inbox: ['M4 13l2-8h12l2 8M4 13v6a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-6M4 13h5l1 2h4l1-2h5'],
  image: [{ r: [4, 5, 16, 14, 2] }, { c: [9, 10, 1.5] }, 'M20 16l-4.5-4.5L7 19'],
  'file-text': [
    'M7 3h7l5 5v12a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z',
    'M14 3v5h5M9 13h6M9 17h6',
  ],
  chat: ['M5 5h14a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H10l-5 4v-4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z'],
  producto: ['M12 3l8 4.5v9L12 21l-8-4.5v-9z', 'M4 7.5l8 4.5 8-4.5M12 12v9'],
  almacen: ['M3 9.5L12 4l9 5.5V20H3z', 'M8 20v-7h8v7M8 16.5h8'],
  tienda: [
    'M3.5 9.5L5 4h14l1.5 5.5M3.5 9.5a2.8 2.8 0 0 0 5.5 0 2.8 2.8 0 0 0 6 0 2.8 2.8 0 0 0 5.5 0M5 13v7h14v-7M10 20v-4h4v4',
  ],
  kardex: [
    'M9 4h6a1 1 0 0 1 1 1v1H8V5a1 1 0 0 1 1-1z',
    'M8 6H6a1 1 0 0 0-1 1v13a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V7a1 1 0 0 0-1-1h-2M9 12h6M9 16h4',
  ],
  movimiento: ['M8 5v14M4 15l4 4 4-4M16 19V5M12 9l4-4 4 4'],
  traslado: ['M4 8h14M14 4l4 4-4 4M20 16H6M10 12l-4 4 4 4'],
  despacho: [
    'M3 6h11v9.5H3z',
    'M14 9.5h4l3 3.5v2.5h-7',
    { c: [7.5, 17.5, 2] },
    { c: [17.5, 17.5, 2] },
  ],
  recepcion: ['M4 8l8-4.5L20 8v8.5L12 21 4 16.5z', 'M12 8v6M9.5 11.5L12 14l2.5-2.5'],
  'orden-compra': [
    'M7 3h7l5 5v12a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z',
    'M14 3v5h5M12 12v5M9.5 14.5h5',
  ],
  cotizacion: [
    'M7 3h7l5 5v12a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z',
    'M14 3v5h5M9 15l2 2 4-4',
  ],
  venta: ['M3.5 5h2.2l2.1 10h9.4l2-7.5H7', { c: [9.5, 19, 1.3] }, { c: [16.5, 19, 1.3] }],
  proveedor: ['M3 20V10l5 3v-3l5 3V5h8v15z', 'M3 20h18M16.5 9h1M16.5 13h1'],
  scan: [
    'M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3M4 12h16',
  ],
  reserva: ['M7 4h10a1 1 0 0 1 1 1v15l-6-4-6 4V5a1 1 0 0 1 1-1z'],
  rol: ['M12 3l8 3v6c0 4.5-3.2 8-8 9-4.8-1-8-4.5-8-9V6z', 'M9 12l2 2 4-4'],
  sku: ['M4 6v12M7 6v12M10.5 6v12M14 6v12M17 6v12M20 6v12'],
} satisfies Record<string, Shape[]>;

export type IconName = keyof typeof ICONS;
